package com.codesentinel.analyzer.rules;

import com.codesentinel.analyzer.model.CodeIssue;
import com.codesentinel.model.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class InfiniteLoopDetector {
    private static final Pattern WHILE_TRUE = Pattern.compile("\\bwhile\\s*\\(\\s*(true|1\\s*==\\s*1)\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOR_EMPTY = Pattern.compile("\\bfor\\s*\\(\\s*;\\s*;\\s*\\)");
    private static final Pattern LOOP = Pattern.compile("\\b(while|for)\\s*\\(");
    private static final Pattern EXIT = Pattern.compile("\\b(break|return|throw|System\\.exit|process\\.exit|exit\\s*\\()", Pattern.CASE_INSENSITIVE);

    public List<CodeIssue> detect(String code) {
        List<CodeIssue> issues = new ArrayList<>();
        String[] lines = code.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (WHILE_TRUE.matcher(line).find() || FOR_EMPTY.matcher(line).find()) {
                issues.add(issue(i + 1, line, "Infinite loop pattern detected."));
                continue;
            }
            if (LOOP.matcher(line).find() && line.contains("{")) {
                String block = blockFrom(lines, i);
                if (block.length() > line.length() && !EXIT.matcher(block).find() && looksUnbounded(line)) {
                    issues.add(issue(i + 1, line, "Loop appears to have no break, return, throw, or exit path."));
                }
            }
        }
        return issues;
    }

    private boolean looksUnbounded(String line) {
        String compact = line.replaceAll("\\s+", "");
        return compact.contains("while(") || compact.matches(".*for\\([^;]*;\\s*;.*");
    }

    private String blockFrom(String[] lines, int start) {
        StringBuilder block = new StringBuilder();
        int depth = 0;
        for (int i = start; i < lines.length; i++) {
            for (char ch : lines[i].toCharArray()) {
                if (ch == '{') depth++;
                if (ch == '}') depth--;
            }
            block.append(lines[i]).append('\n');
            if (i > start && depth <= 0) break;
        }
        return block.toString();
    }

    private CodeIssue issue(int line, String snippet, String detail) {
        return new CodeIssue(line, "Infinite Loop", Severity.CRITICAL, detail,
                "Add a bounded condition or a guaranteed break/return path.", snippet.trim());
    }
}
