package com.codesentinel.analyzer.rules;

import com.codesentinel.analyzer.model.CodeIssue;
import com.codesentinel.model.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfiniteLoopDetector {
    private static final Pattern WHILE_TRUE = Pattern.compile("\\bwhile\\s*\\(\\s*(true|1\\s*==\\s*1)\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOR_EMPTY = Pattern.compile("\\bfor\\s*\\(\\s*;\\s*;\\s*\\)");
    private static final Pattern FOR_LOOP = Pattern.compile("\\bfor\\s*\\(([^;]*);([^;]*);([^)]*)\\)");
    private static final Pattern EXIT = Pattern.compile("\\b(break|return|throw|System\\.exit|process\\.exit|exit\\s*\\()", Pattern.CASE_INSENSITIVE);

    public List<CodeIssue> detect(String code) {
        List<CodeIssue> issues = new ArrayList<>();
        String[] lines = CodeSanitizer.stripCommentsAndStrings(code).split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (WHILE_TRUE.matcher(line).find() || FOR_EMPTY.matcher(line).find()) {
                String block = blockFrom(lines, i);
                if (!EXIT.matcher(block).find()) {
                    issues.add(issue(i + 1, line, "Loop has an unconditional infinite-loop header and no visible exit path."));
                }
                continue;
            }
            Matcher forLoop = FOR_LOOP.matcher(line);
            if (forLoop.find() && forLoop.group(3).trim().isEmpty()) {
                String block = blockFrom(lines, i);
                if (!EXIT.matcher(block).find() && loopConditionCanContinue(forLoop.group(2))) {
                    issues.add(issue(i + 1, line,
                            "For loop has a continuing condition but no update expression or visible exit path."));
                }
            }
        }
        return issues;
    }

    private boolean loopConditionCanContinue(String condition) {
        String trimmed = condition.trim();
        return trimmed.isEmpty() || !trimmed.equals("false");
    }

    private String blockFrom(String[] lines, int start) {
        StringBuilder block = new StringBuilder();
        int depth = 0;
        boolean sawOpenBrace = false;
        for (int i = start; i < lines.length; i++) {
            for (char ch : lines[i].toCharArray()) {
                if (ch == '{') {
                    depth++;
                    sawOpenBrace = true;
                }
                if (ch == '}') depth--;
            }
            block.append(lines[i]).append('\n');
            if (sawOpenBrace && depth <= 0) break;
        }
        return block.toString();
    }

    private CodeIssue issue(int line, String snippet, String detail) {
        return new CodeIssue(line, "Infinite Loop", Severity.MAJOR, detail,
                "Add the missing loop update, for example: for (int i = 0; i < 5; i++) { ... }",
                snippet.trim(), "High Confidence");
    }
}
