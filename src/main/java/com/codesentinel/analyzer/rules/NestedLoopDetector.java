package com.codesentinel.analyzer.rules;

import com.codesentinel.analyzer.model.CodeIssue;
import com.codesentinel.model.Severity;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NestedLoopDetector {
    private static final Pattern LOOP = Pattern.compile("\\b(for|while|do)\\b", Pattern.CASE_INSENSITIVE);

    public List<CodeIssue> detect(String code) {
        List<CodeIssue> issues = new ArrayList<>();
        String clean = CodeSanitizer.stripCommentsAndStrings(code);
        Deque<Integer> loopDepths = new ArrayDeque<>();
        boolean pendingLoop = false;
        int pendingLine = 1;
        int lineNumber = 1;
        int parenDepth = 0;

        for (int i = 0; i < clean.length(); i++) {
            Matcher matcher = LOOP.matcher(clean);
            matcher.region(i, clean.length());
            if (matcher.lookingAt()) {
                pendingLoop = true;
                pendingLine = lineNumber;
                i = matcher.end() - 1;
                continue;
            }

            char ch = clean.charAt(i);
            if (ch == '\n') lineNumber++;
            if (ch == '(') parenDepth++;
            if (ch == ')') parenDepth = Math.max(0, parenDepth - 1);
            if (ch == '{') {
                int depth = loopDepths.isEmpty() ? 0 : loopDepths.peek();
                if (pendingLoop) {
                    depth++;
                    addIssueIfNeeded(issues, depth, pendingLine, lineSnippet(code, pendingLine));
                }
                loopDepths.push(depth);
                pendingLoop = false;
                continue;
            }
            if (ch == '}') {
                if (!loopDepths.isEmpty()) loopDepths.pop();
                pendingLoop = false;
                continue;
            }
            if (ch == ';' && parenDepth == 0) {
                if (pendingLoop) {
                    int depth = (loopDepths.isEmpty() ? 0 : loopDepths.peek()) + 1;
                    addIssueIfNeeded(issues, depth, pendingLine, lineSnippet(code, pendingLine));
                }
                pendingLoop = false;
            }
        }
        return issues;
    }

    private void addIssueIfNeeded(List<CodeIssue> issues, int loopDepth, int lineNumber, String snippet) {
        if (loopDepth >= 3) {
            issues.add(new CodeIssue(lineNumber, "Nested Loop", Severity.CRITICAL,
                    "Nested loop depth is " + loopDepth + ", which can cause performance and readability problems.",
                    "Extract helper methods, pre-index data, or restructure loops to reduce nesting.",
                    snippet.trim()));
        }
    }

    private String lineSnippet(String code, int lineNumber) {
        String[] lines = code.split("\\R", -1);
        return lineNumber > 0 && lineNumber <= lines.length ? lines[lineNumber - 1] : "";
    }
}
