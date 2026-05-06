package com.codesentinel.analyzer.rules;

import com.codesentinel.analyzer.model.CodeIssue;
import com.codesentinel.model.Severity;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

public class NestedLoopDetector {
    private static final Pattern LOOP = Pattern.compile("\\b(for|while|do)\\b", Pattern.CASE_INSENSITIVE);

    public List<CodeIssue> detect(String code) {
        List<CodeIssue> issues = new ArrayList<>();
        String[] lines = code.split("\\R", -1);
        Deque<Integer> loopBraceDepths = new ArrayDeque<>();
        int braceDepth = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int closeCount = count(line, '}');
            for (int c = 0; c < closeCount; c++) {
                braceDepth = Math.max(0, braceDepth - 1);
                while (!loopBraceDepths.isEmpty() && loopBraceDepths.peekLast() > braceDepth) {
                    loopBraceDepths.removeLast();
                }
            }
            if (LOOP.matcher(line).find()) {
                int loopDepth = loopBraceDepths.size() + 1;
                if (loopDepth >= 3) {
                    issues.add(new CodeIssue(i + 1, "Nested Loop", Severity.CRITICAL,
                            "Nested loop depth is " + loopDepth + ", which can cause performance and readability problems.",
                            "Extract helper methods, pre-index data, or restructure loops to reduce nesting.",
                            line.trim()));
                }
                loopBraceDepths.addLast(braceDepth + Math.max(1, count(line, '{')));
            }
            braceDepth += count(line, '{');
        }
        return issues;
    }

    private int count(String s, char c) {
        int n = 0;
        for (char ch : s.toCharArray()) if (ch == c) n++;
        return n;
    }
}
