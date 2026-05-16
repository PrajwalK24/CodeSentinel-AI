package com.codesentinel.analyzer.rules;

import com.codesentinel.model.RiskLevel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComplexityCalculator {
    private static final Pattern DECISION = Pattern.compile("\\b(if|for|while|case|catch)\\b|&&|\\|\\||\\?", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOOP = Pattern.compile("\\b(for|while|do)\\b", Pattern.CASE_INSENSITIVE);

    public int calculate(String code) {
        Matcher matcher = DECISION.matcher(CodeSanitizer.stripCommentsAndStrings(code));
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
        String clean = CodeSanitizer.stripCommentsAndStrings(code);
        int maxLoopDepth = maxLoopDepth(clean);
        if (hasDivideAndConquerShape(clean)) return maxLoopDepth > 0 ? "O(n log n)" : "O(log n)";
        if (maxLoopDepth >= 3) return "O(n^" + maxLoopDepth + ")";
        if (maxLoopDepth == 2) return "O(n^2)";
        if (maxLoopDepth == 1 || hasLinearCollectionTraversal(clean)) return "O(n)";
        if (hasRecursiveCall(clean)) return "O(n)";
        return "O(1)";
    }

    public String estimateSpaceComplexity(String code) {
        String compact = CodeSanitizer.stripCommentsAndStrings(code);
        int collectionCount = countMatches(compact, "\\b(new\\s+(ArrayList|HashMap|HashSet|LinkedList|List|Map|Set)|\\[\\s*\\]|\\.push\\(|\\.add\\(|\\.put\\()");
        if (collectionCount >= 3 || compact.matches("(?s).*\\b\\w+\\s*\\[\\s*\\]\\s*=\\s*new\\s+\\w+\\s*\\[[^\\]]+\\].*")) {
            return "O(n)";
        }
        if (hasRecursiveCall(compact)) {
            return "O(n)";
        }
        return "O(1)";
    }

    private int maxLoopDepth(String code) {
        int maxDepth = 0;
        java.util.Deque<Block> stack = new java.util.ArrayDeque<>();
        boolean pendingLoop = false;
        int parenDepth = 0;

        for (int i = 0; i < code.length(); i++) {
            Matcher matcher = LOOP.matcher(code);
            matcher.region(i, code.length());
            if (matcher.lookingAt()) {
                pendingLoop = true;
                i = matcher.end() - 1;
                continue;
            }

            char ch = code.charAt(i);
            if (ch == '(') parenDepth++;
            if (ch == ')') parenDepth = Math.max(0, parenDepth - 1);
            if (ch == '{') {
                int depth = stack.isEmpty() ? 0 : stack.peek().loopDepth;
                if (pendingLoop) {
                    depth++;
                    maxDepth = Math.max(maxDepth, depth);
                }
                stack.push(new Block(depth));
                pendingLoop = false;
                continue;
            }
            if (ch == '}') {
                if (!stack.isEmpty()) stack.pop();
                pendingLoop = false;
                continue;
            }
            if (ch == ';' && parenDepth == 0) {
                if (pendingLoop) {
                    int depth = (stack.isEmpty() ? 0 : stack.peek().loopDepth) + 1;
                    maxDepth = Math.max(maxDepth, depth);
                }
                pendingLoop = false;
            }
        }
        return maxDepth;
    }

    private boolean hasDivideAndConquerShape(String code) {
        String compact = code.replaceAll("\\s+", "");
        return compact.contains("/2") || compact.contains(">>1") || compact.matches("(?s).*(binarySearch|mergeSort|quickSort).*");
    }

    private boolean hasLinearCollectionTraversal(String code) {
        return Pattern.compile("\\.\\s*(stream|forEach|map|filter|reduce)\\s*\\(", Pattern.CASE_INSENSITIVE).matcher(code).find();
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

    private static class Block {
        private final int loopDepth;

        private Block(int loopDepth) {
            this.loopDepth = loopDepth;
        }
    }
}
