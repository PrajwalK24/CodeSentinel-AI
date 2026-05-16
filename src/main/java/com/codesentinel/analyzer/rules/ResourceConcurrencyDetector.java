package com.codesentinel.analyzer.rules;

import com.codesentinel.analyzer.model.CodeIssue;
import com.codesentinel.model.Severity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResourceConcurrencyDetector {
    private static final Pattern RESOURCE_CREATE = Pattern.compile("\\bnew\\s+(FileInputStream|FileOutputStream|FileReader|FileWriter|BufferedReader|BufferedWriter|Socket)\\s*\\(");
    private static final Pattern STATIC_INT_FIELD = Pattern.compile("\\bstatic\\s+(?:int|long)\\s+([a-zA-Z_]\\w*)\\b");
    private static final Pattern SYNCHRONIZED_LOCK = Pattern.compile("\\bsynchronized\\s*\\(\\s*([a-zA-Z_]\\w*)\\s*\\)");

    public List<CodeIssue> detect(String code) {
        List<CodeIssue> issues = new ArrayList<>();
        String[] originalLines = code.split("\\R", -1);
        String clean = CodeSanitizer.stripCommentsAndStrings(code);
        String[] lines = clean.split("\\R", -1);

        detectResourceLeaks(issues, originalLines, lines);
        detectDeadlocks(issues, originalLines, lines);
        detectSharedCounterRace(issues, originalLines, clean);
        return issues;
    }

    private void detectResourceLeaks(List<CodeIssue> issues, String[] originalLines, String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            if (!RESOURCE_CREATE.matcher(lines[i]).find()) continue;
            String nearby = nearby(lines, i, 10);
            boolean tryWithResources = nearby.matches("(?s).*try\\s*\\([^)]*new\\s+(FileInputStream|FileOutputStream|FileReader|FileWriter|BufferedReader|BufferedWriter|Socket).*");
            boolean closes = nearby.matches("(?s).*\\.close\\s*\\(\\s*\\).*");
            if (!tryWithResources && !closes) {
                issues.add(new CodeIssue(i + 1, "Resource Leak", Severity.MAJOR,
                        "A closeable resource is created without a visible close or try-with-resources block, so file/socket handles may leak.",
                        "Use try-with-resources, for example: try (BufferedReader br = new BufferedReader(...)) { ... }",
                        originalLines[i].trim(), "Medium Confidence"));
            }
        }
    }

    private void detectDeadlocks(List<CodeIssue> issues, String[] originalLines, String[] lines) {
        Set<String> lockPairs = new HashSet<>();
        for (int i = 0; i < lines.length; i++) {
            Matcher outer = SYNCHRONIZED_LOCK.matcher(lines[i]);
            if (!outer.find()) continue;
            String first = outer.group(1);
            String block = nearby(lines, i, 8);
            Matcher inner = SYNCHRONIZED_LOCK.matcher(block);
            if (!inner.find()) continue;
            while (inner.find()) {
                String second = inner.group(1);
                if (first.equals(second)) continue;
                String pair = first + "->" + second;
                String reverse = second + "->" + first;
                if (lockPairs.contains(reverse)) {
                    issues.add(new CodeIssue(i + 1, "Potential Deadlock", Severity.CRITICAL,
                            "The same locks are acquired in opposite order elsewhere, which can deadlock when two threads enter both paths.",
                            "Always acquire locks in one global order, for example lock " + second + " before " + first + " everywhere.",
                            originalLines[i].trim(), "High Confidence"));
                    return;
                }
                lockPairs.add(pair);
            }
        }
    }

    private void detectSharedCounterRace(List<CodeIssue> issues, String[] originalLines, String clean) {
        if (!clean.contains("new Thread") && !clean.contains("implements Runnable") && !clean.contains("extends Thread")) return;
        Matcher field = STATIC_INT_FIELD.matcher(clean);
        while (field.find()) {
            String name = field.group(1);
            Pattern update = Pattern.compile("\\b" + Pattern.quote(name) + "\\s*(\\+\\+|--|[+\\-*/%]?=)");
            Matcher updateMatch = update.matcher(clean);
            if (updateMatch.find() && !clean.contains("synchronized") && !clean.contains("AtomicInteger") && !clean.contains("AtomicLong")) {
                int line = lineNumber(clean, updateMatch.start());
                issues.add(new CodeIssue(line, "Race Condition", Severity.MAJOR,
                        "A shared static numeric field is updated in threaded code without synchronization or an atomic type.",
                        "Use AtomicInteger/AtomicLong or guard the update with synchronized/Lock.",
                        lineAt(originalLines, line), "Medium Confidence"));
                return;
            }
        }
    }

    private String nearby(String[] lines, int start, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < Math.min(lines.length, start + count); i++) {
            builder.append(lines[i]).append('\n');
        }
        return builder.toString();
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
}
