package com.codesentinel.analyzer.rules;

import com.codesentinel.model.RiskLevel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComplexityCalculator {
    private static final Pattern DECISION = Pattern.compile("\\b(if|else\\s+if|for|while|case|catch)\\b|&&|\\|\\|", Pattern.CASE_INSENSITIVE);

    public int calculate(String code) {
        Matcher matcher = DECISION.matcher(stripStrings(code));
        int score = 1;
        while (matcher.find()) {
            score++;
        }
        return score;
    }

    public RiskLevel riskFor(int score) {
        if (score >= 21) return RiskLevel.CRITICAL;
        if (score >= 11) return RiskLevel.HIGH;
        if (score >= 6) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    public String estimateTimeComplexity(String code) {
        String[] lines = code.split("\\R", -1);
        int maxLoopDepth = maxLoopDepth(lines);
        if (hasDivideAndConquerShape(code)) return maxLoopDepth > 0 ? "O(n log n)" : "O(log n)";
        if (maxLoopDepth >= 3) return "O(n^3)";
        if (maxLoopDepth == 2) return "O(n^2)";
        if (maxLoopDepth == 1) return "O(n)";
        return "O(1)";
    }

    public String estimateSpaceComplexity(String code) {
        String compact = stripStrings(code);
        int collectionCount = countMatches(compact, "\\b(new\\s+(ArrayList|HashMap|HashSet|LinkedList|List|Map|Set)|\\[\\s*\\]|\\.push\\(|\\.add\\(|\\.put\\()");
        if (collectionCount >= 3 || compact.matches("(?s).*\\b\\w+\\s*\\[\\s*\\]\\s*=\\s*new\\s+\\w+\\s*\\[[^\\]]+\\].*")) {
            return "O(n)";
        }
        if (hasRecursiveCall(compact)) {
            return "O(n)";
        }
        return "O(1)";
    }

    private int maxLoopDepth(String[] lines) {
        int braceDepth = 0;
        int maxDepth = 0;
        java.util.Deque<Integer> loopBraceDepths = new java.util.ArrayDeque<>();
        Pattern loop = Pattern.compile("\\b(for|while|do)\\b", Pattern.CASE_INSENSITIVE);
        for (String line : lines) {
            int closeCount = count(line, '}');
            for (int i = 0; i < closeCount; i++) {
                braceDepth = Math.max(0, braceDepth - 1);
                while (!loopBraceDepths.isEmpty() && loopBraceDepths.peekLast() > braceDepth) {
                    loopBraceDepths.removeLast();
                }
            }
            if (loop.matcher(line).find()) {
                int loopDepth = loopBraceDepths.size() + 1;
                maxDepth = Math.max(maxDepth, loopDepth);
                loopBraceDepths.addLast(braceDepth + Math.max(1, count(line, '{')));
            }
            braceDepth += count(line, '{');
        }
        return maxDepth;
    }

    private boolean hasDivideAndConquerShape(String code) {
        String compact = code.replaceAll("\\s+", "");
        return compact.contains("/2") || compact.contains(">>1") || compact.matches("(?s).*(binarySearch|mergeSort|quickSort).*");
    }

    private boolean hasRecursiveCall(String code) {
        Matcher method = Pattern.compile("\\b(?:public|private|protected)?\\s*(?:static\\s+)?[\\w<>\\[\\]]+\\s+(\\w+)\\s*\\(").matcher(code);
        while (method.find()) {
            String name = method.group(1);
            int next = code.indexOf(name + "(", method.end());
            if (next > method.end()) return true;
        }
        return false;
    }

    private int countMatches(String code, String regex) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(code);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private int count(String s, char c) {
        int n = 0;
        for (char ch : s.toCharArray()) if (ch == c) n++;
        return n;
    }

    private String stripStrings(String code) {
        return code.replaceAll("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'", "\"\"");
    }
}
