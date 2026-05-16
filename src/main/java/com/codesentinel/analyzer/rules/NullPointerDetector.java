package com.codesentinel.analyzer.rules;

import com.codesentinel.analyzer.model.CodeIssue;
import com.codesentinel.model.Severity;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NullPointerDetector {
    private static final Pattern METHOD_CALL = Pattern.compile("\\b([a-zA-Z_][\\w]*)\\s*\\.\\s*[a-zA-Z_]\\w*\\s*\\(");
    private static final Pattern NULL_GUARD = Pattern.compile("\\b([a-zA-Z_][\\w]*)\\s*(?:!=\\s*null|==\\s*null)|Objects\\.nonNull\\s*\\(\\s*([a-zA-Z_]\\w*)");
    private static final Pattern NULL_ASSIGNMENT = Pattern.compile("\\b([a-zA-Z_][\\w]*)\\s*=\\s*null\\s*;");
    private static final Pattern ASSIGNMENT = Pattern.compile("\\b([a-zA-Z_][\\w]*)\\s*=\\s*([^;]+);");
    private static final Pattern ARRAY_ACCESS = Pattern.compile("\\b([a-zA-Z_][\\w]*)\\s*\\[\\s*([^\\]]+)\\s*\\]");
    private static final Pattern ARRAY_LITERAL_DECL = Pattern.compile("\\b([a-zA-Z_][\\w]*)\\s*\\[\\s*]\\s+([a-zA-Z_][\\w]*)\\s*=\\s*\\{([^}]*)}");
    private static final Pattern ARRAY_NEW_DECL = Pattern.compile("\\b([a-zA-Z_][\\w]*)\\s*\\[\\s*]\\s+([a-zA-Z_][\\w]*)\\s*=\\s*new\\s+[a-zA-Z_][\\w]*\\s*\\[\\s*(\\d+)\\s*]");
    private static final Set<String> ARRAY_TYPE_WORDS = Set.of(
            "byte", "short", "int", "long", "float", "double", "char", "boolean", "String");
    private static final Set<String> SAFE_QUALIFIERS = Set.of(
            "System", "Objects", "String", "Math", "Collections", "List", "Map", "Optional",
            "Runtime", "Pattern", "Files", "Paths", "this", "super", "out", "err", "console", "logger", "log");

    public List<CodeIssue> detect(String code) {
        List<CodeIssue> issues = new ArrayList<>();
        String[] lines = code.split("\\R", -1);
        String[] cleanLines = CodeSanitizer.stripComments(code).split("\\R", -1);
        Map<String, Integer> knownArrayLengths = discoverArrayLengths(cleanLines);
        Set<String> knownNulls = new HashSet<>();
        Deque<String> recentGuards = new ArrayDeque<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String cleanLine = cleanLines[i];
            Matcher guard = NULL_GUARD.matcher(line);
            while (guard.find()) {
                String guarded = Optional.ofNullable(guard.group(1)).orElse(guard.group(2));
                recentGuards.addLast(guarded);
                knownNulls.remove(guarded);
            }
            while (recentGuards.size() > 12) recentGuards.removeFirst();

            Matcher nullAssignment = NULL_ASSIGNMENT.matcher(cleanLine);
            while (nullAssignment.find()) {
                knownNulls.add(nullAssignment.group(1));
            }

            Matcher call = METHOD_CALL.matcher(line);
            while (call.find()) {
                String object = call.group(1);
                String nullObject = dereferencedNullObject(object, line, knownNulls);
                if (nullObject != null) {
                    issues.add(new CodeIssue(i + 1, "Null Pointer Dereference", Severity.CRITICAL,
                            "Object '" + nullObject + "' is definitely null when it is dereferenced.",
                            "Initialize the object before use or guard this path before calling methods on it.",
                            line.trim()));
                    break;
                }
            }

            Matcher assignment = ASSIGNMENT.matcher(cleanLine);
            while (assignment.find()) {
                if (!assignment.group(2).trim().equals("null")) {
                    knownNulls.remove(assignment.group(1));
                }
            }

            Matcher array = ARRAY_ACCESS.matcher(line);
            while (array.find()) {
                String arrayName = array.group(1);
                String indexExpression = array.group(2).trim();
                ArrayAccessRisk risk = arrayAccessRisk(lines, i, line, arrayName, indexExpression, knownArrayLengths);
                if (risk == ArrayAccessRisk.OUT_OF_BOUNDS) {
                    issues.add(new CodeIssue(i + 1, "Array Index Out Of Bounds", Severity.CRITICAL,
                            "The constant array index is outside the known array length.",
                            "Use an index between 0 and length - 1 for this array.",
                            line.trim()));
                    break;
                }
            }
        }
        return issues;
    }

    private boolean isFrameworkOutputChain(String line, String object) {
        String compact = line.replaceAll("\\s+", "");
        return compact.contains("System." + object + ".")
                || compact.contains("console." + object + ".")
                || compact.contains("logger." + object + ".")
                || compact.contains("log." + object + ".");
    }

    private String dereferencedNullObject(String matchedObject, String line, Set<String> knownNulls) {
        if (knownNulls.contains(matchedObject)) {
            return matchedObject;
        }
        for (String knownNull : knownNulls) {
            if (Pattern.compile("\\b" + Pattern.quote(knownNull) + "\\s*\\.\\s*[a-zA-Z_]\\w*\\s*\\(").matcher(line).find()) {
                return knownNull;
            }
        }
        return null;
    }

    private Map<String, Integer> discoverArrayLengths(String[] lines) {
        Map<String, Integer> lengths = new HashMap<>();
        for (String line : lines) {
            Matcher literal = ARRAY_LITERAL_DECL.matcher(line);
            if (literal.find()) {
                lengths.put(literal.group(2), countArrayLiteralItems(literal.group(3)));
            }
            Matcher created = ARRAY_NEW_DECL.matcher(line);
            if (created.find()) {
                lengths.put(created.group(2), Integer.parseInt(created.group(3)));
            }
        }
        return lengths;
    }

    private int countArrayLiteralItems(String body) {
        String trimmed = body.trim();
        if (trimmed.isEmpty()) return 0;
        return trimmed.split(",").length;
    }

    private ArrayAccessRisk arrayAccessRisk(String[] lines, int i, String line, String arrayName, String indexExpression, Map<String, Integer> knownArrayLengths) {
        if (ARRAY_TYPE_WORDS.contains(arrayName) || line.contains(".length")) {
            return ArrayAccessRisk.SAFE;
        }
        Integer knownLength = knownArrayLengths.get(arrayName);
        Integer constantIndex = parseInteger(indexExpression);
        if (knownLength != null && constantIndex != null) {
            return constantIndex >= 0 && constantIndex < knownLength ? ArrayAccessRisk.SAFE : ArrayAccessRisk.OUT_OF_BOUNDS;
        }
        if (knownLength != null && isSimpleLoopIndex(indexExpression, lines, i, arrayName)) {
            return ArrayAccessRisk.SAFE;
        }
        String nearby = nearbyCode(lines, i, 10);
        String compact = nearby.replaceAll("\\s+", "");
        String compactArray = Pattern.quote(arrayName);
        if (compact.contains(arrayName + ".length")) {
            return ArrayAccessRisk.SAFE;
        }
        if (compact.matches("(?s).*" + compactArray + "\\[[^\\]]+\\]\\.length.*")) {
            return ArrayAccessRisk.SAFE;
        }
        if (compact.matches("(?s).*(?:if|while)\\([^)]*" + compactArray + "[^)]*\\.length[^)]*\\).*")) {
            return ArrayAccessRisk.SAFE;
        }
        return compact.contains(arrayName + "!=null") || compact.contains(arrayName + "==null")
                ? ArrayAccessRisk.SAFE
                : ArrayAccessRisk.UNKNOWN;
    }

    private Integer parseInteger(String expression) {
        try {
            return Integer.parseInt(expression);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isSimpleLoopIndex(String indexExpression, String[] lines, int currentLine, String arrayName) {
        String nearby = nearbyCode(lines, currentLine, 6).replaceAll("\\s+", "");
        String index = Pattern.quote(indexExpression);
        String array = Pattern.quote(arrayName);
        return nearby.matches("(?s).*for\\([^;]*" + index + "=0;[^;]*" + index + "<" + array + "\\.length;.*");
    }

    private String nearbyCode(String[] lines, int i, int lookback) {
        StringBuilder builder = new StringBuilder();
        for (int j = Math.max(0, i - lookback); j <= i; j++) {
            builder.append(lines[j]).append('\n');
        }
        return builder.toString();
    }

    private enum ArrayAccessRisk {
        SAFE,
        UNKNOWN,
        OUT_OF_BOUNDS
    }
}
