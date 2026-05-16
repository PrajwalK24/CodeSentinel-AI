package com.codesentinel.analyzer.rules;

import com.codesentinel.analyzer.model.CodeIssue;
import com.codesentinel.model.Severity;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogicalRuntimeBugDetector {
    private static final Pattern TERMINATOR = Pattern.compile("\\b(return|throw|break|continue)\\b.*;");
    private static final Pattern METHOD_DECL = Pattern.compile("\\b(?:public|private|protected)?\\s*(?:static\\s+)?[\\w<>\\[\\]]+\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{");
    private static final Pattern WHILE_CONDITION = Pattern.compile("\\bwhile\\s*\\(\\s*([a-zA-Z_]\\w*)\\s*(?:<|<=|>|>=|!=)\\s*[^)]*\\)\\s*\\{");
    private static final Pattern FOR_CONSTANT_CONDITION = Pattern.compile("\\bfor\\s*\\(\\s*int\\s+([a-zA-Z_]\\w*)\\s*=\\s*(-?\\d+)\\s*;\\s*\\1\\s*(<=|>=|==|!=|<|>)\\s*(-?\\d+)\\s*;[^)]*\\)\\s*\\{");
    private static final Pattern CONSTANT_IF_WHILE = Pattern.compile("\\b(if|while)\\s*\\(\\s*(-?\\d+)\\s*(<=|>=|==|!=|<|>)\\s*(-?\\d+)\\s*\\)");
    private static final Pattern INT_LITERAL_EXPR = Pattern.compile("\\bint\\s+[a-zA-Z_]\\w*\\s*=\\s*(-?\\d+)\\s*([+\\-*/])\\s*(-?\\d+)\\s*;");
    private static final Pattern MATH_ABS_MIN = Pattern.compile("\\bMath\\.abs\\s*\\(\\s*Integer\\.MIN_VALUE\\s*\\)");

    public List<CodeIssue> detect(String code) {
        List<CodeIssue> issues = new ArrayList<>();
        String clean = CodeSanitizer.stripCommentsAndStrings(code);
        String[] originalLines = code.split("\\R", -1);
        String[] lines = clean.split("\\R", -1);

        detectUnreachableCode(issues, originalLines, lines);
        detectLiteralIntegerOverflow(issues, originalLines, lines);
        detectInfiniteRecursion(issues, originalLines, clean);
        detectMissingLoopUpdates(issues, originalLines, lines);
        detectNeverExecutingLoops(issues, originalLines, lines);
        detectConstantConditions(issues, originalLines, lines);
        return issues;
    }

    private void detectUnreachableCode(List<CodeIssue> issues, String[] originalLines, String[] lines) {
        boolean previousTerminates = false;
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.isEmpty()) continue;
            if (previousTerminates && !trimmed.startsWith("}") && !trimmed.startsWith("case ") && !trimmed.startsWith("default:")) {
                issues.add(new CodeIssue(i + 1, "Unreachable Code", Severity.MAJOR,
                        "This statement is placed after a return/throw/break/continue in the same block, so execution cannot reach it.",
                        "Move the statement before the terminating statement or remove it if it is unnecessary.",
                        originalLines[i].trim(), "High Confidence"));
                previousTerminates = false;
            }
            previousTerminates = TERMINATOR.matcher(trimmed).matches();
        }
    }

    private void detectLiteralIntegerOverflow(List<CodeIssue> issues, String[] originalLines, String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            Matcher expr = INT_LITERAL_EXPR.matcher(lines[i]);
            if (expr.find()) {
                BigInteger left = new BigInteger(expr.group(1));
                BigInteger right = new BigInteger(expr.group(3));
                BigInteger value = switch (expr.group(2)) {
                    case "+" -> left.add(right);
                    case "-" -> left.subtract(right);
                    case "*" -> left.multiply(right);
                    case "/" -> right.equals(BigInteger.ZERO) ? BigInteger.ZERO : left.divide(right);
                    default -> BigInteger.ZERO;
                };
                if (value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0
                        || value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {
                    issues.add(new CodeIssue(i + 1, "Integer Overflow", Severity.MAJOR,
                            "This constant int expression evaluates outside the Java int range and will overflow or fail correctness expectations.",
                            "Use long literals, for example: long value = " + expr.group(1) + "L " + expr.group(2) + " " + expr.group(3) + "L;",
                            originalLines[i].trim(), "High Confidence"));
                }
            }
            if (MATH_ABS_MIN.matcher(lines[i]).find()) {
                issues.add(new CodeIssue(i + 1, "Integer Overflow", Severity.MAJOR,
                        "Math.abs(Integer.MIN_VALUE) still returns a negative value because the positive value exceeds int range.",
                        "Cast before abs: long safe = Math.abs((long) Integer.MIN_VALUE);",
                        originalLines[i].trim(), "High Confidence"));
            }
        }
    }

    private void detectInfiniteRecursion(List<CodeIssue> issues, String[] originalLines, String clean) {
        Matcher method = METHOD_DECL.matcher(clean);
        while (method.find()) {
            String name = method.group(1);
            int startBrace = clean.indexOf('{', method.end() - 1);
            int endBrace = matchingBrace(clean, startBrace);
            if (startBrace < 0 || endBrace <= startBrace) continue;
            String body = clean.substring(startBrace + 1, endBrace).trim();
            String compact = body.replaceAll("\\s+", "");
            boolean directOnly = compact.matches("(return)?" + Pattern.quote(name) + "\\([^;]*\\);?");
            if (directOnly || (!body.contains("if") && body.matches("(?s).*\\b" + Pattern.quote(name) + "\\s*\\([^;]*\\).*"))) {
                int line = lineNumber(clean, method.start());
                issues.add(new CodeIssue(line, "Infinite Recursion", Severity.MAJOR,
                        "The method calls itself without an evident base condition, so it can recurse until stack overflow.",
                        "Add a reachable base case before the recursive call, for example: if (n <= 0) return result;",
                        lineAt(originalLines, line), "Medium Confidence"));
            }
        }
    }

    private void detectMissingLoopUpdates(List<CodeIssue> issues, String[] originalLines, String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            Matcher loop = WHILE_CONDITION.matcher(lines[i]);
            if (!loop.find()) continue;
            String variable = loop.group(1);
            String block = blockFrom(lines, i);
            if (block.matches("(?s).*\\b(break|return|throw)\\b.*")) continue;
            if (!Pattern.compile("\\b" + Pattern.quote(variable) + "\\s*(\\+\\+|--|[+\\-*/%]?=)").matcher(block).find()) {
                issues.add(new CodeIssue(i + 1, "Missing Loop Update", Severity.MAJOR,
                        "The while condition depends on '" + variable + "', but the loop body does not update it or exit.",
                        "Update the loop variable inside the loop, for example: " + variable + "++;",
                        originalLines[i].trim(), "Medium Confidence"));
            }
        }
    }

    private void detectNeverExecutingLoops(List<CodeIssue> issues, String[] originalLines, String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            Matcher loop = FOR_CONSTANT_CONDITION.matcher(lines[i]);
            if (!loop.find()) continue;
            int initial = Integer.parseInt(loop.group(2));
            int bound = Integer.parseInt(loop.group(4));
            if (!conditionIsTrue(initial, loop.group(3), bound)) {
                issues.add(new CodeIssue(i + 1, "Loop Never Executes", Severity.MAJOR,
                        "The initial value makes the loop condition false before the first iteration, so the loop body is unreachable.",
                        "Fix the condition or initial value, for example: for (int j = 10; j > 5; j--) { ... }",
                        originalLines[i].trim(), "High Confidence"));
            }
        }
    }

    private void detectConstantConditions(List<CodeIssue> issues, String[] originalLines, String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            Matcher condition = CONSTANT_IF_WHILE.matcher(lines[i]);
            if (!condition.find()) continue;
            int left = Integer.parseInt(condition.group(2));
            int right = Integer.parseInt(condition.group(4));
            boolean result = conditionIsTrue(left, condition.group(3), right);
            String keyword = condition.group(1);
            String issueType = result ? "Condition Always True" : "Condition Always False";
            Severity severity = keyword.equals("while") && result ? Severity.MAJOR : Severity.MINOR;
            String impact = keyword.equals("while") && result
                    ? "The loop can run forever unless there is an internal exit path."
                    : "One branch is dead, so intended logic may silently never execute.";
            issues.add(new CodeIssue(i + 1, issueType, severity,
                    "The condition uses only constants and always evaluates to " + result + ".",
                    "Replace the constant expression with the intended variable comparison or remove the dead branch.",
                    originalLines[i].trim(), impact, "High Confidence"));
        }
    }

    private boolean conditionIsTrue(int left, String operator, int right) {
        return switch (operator) {
            case "<" -> left < right;
            case "<=" -> left <= right;
            case ">" -> left > right;
            case ">=" -> left >= right;
            case "==" -> left == right;
            case "!=" -> left != right;
            default -> true;
        };
    }

    private int matchingBrace(String code, int open) {
        int depth = 0;
        for (int i = open; i < code.length(); i++) {
            if (code.charAt(i) == '{') depth++;
            if (code.charAt(i) == '}') depth--;
            if (depth == 0) return i;
        }
        return -1;
    }

    private int lineNumber(String code, int offset) {
        int line = 1;
        for (int i = 0; i < offset && i < code.length(); i++) {
            if (code.charAt(i) == '\n') line++;
        }
        return line;
    }

    private String lineAt(String[] lines, int oneBasedLine) {
        return oneBasedLine > 0 && oneBasedLine <= lines.length ? lines[oneBasedLine - 1].trim() : "";
    }

    private String blockFrom(String[] lines, int start) {
        StringBuilder block = new StringBuilder();
        int depth = 0;
        boolean sawBrace = false;
        for (int i = start; i < lines.length; i++) {
            for (char ch : lines[i].toCharArray()) {
                if (ch == '{') {
                    depth++;
                    sawBrace = true;
                }
                if (ch == '}') depth--;
            }
            block.append(lines[i]).append('\n');
            if (sawBrace && depth <= 0) break;
        }
        return block.toString();
    }
}
