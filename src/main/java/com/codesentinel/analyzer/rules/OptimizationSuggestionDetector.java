package com.codesentinel.analyzer.rules;

import com.codesentinel.analyzer.model.CodeIssue;
import com.codesentinel.model.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class OptimizationSuggestionDetector {
    private static final Pattern STRING_CONCAT = Pattern.compile("\\w+\\s*=\\s*\\w+\\s*\\+");
    private static final Pattern REGEX_COMPILE = Pattern.compile("\\bPattern\\.compile\\s*\\(");
    private static final Pattern LINEAR_CONTAINS = Pattern.compile("\\.contains\\s*\\(");
    private static final Pattern SORT_IN_LOOP = Pattern.compile("\\b(Collections\\.sort|Arrays\\.sort|\\.sort\\s*\\()\\b");

    public List<CodeIssue> detect(String code) {
        List<CodeIssue> issues = new ArrayList<>();
        String[] lines = CodeSanitizer.stripCommentsAndStrings(code).split("\\R", -1);
        int loopDepth = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int closing = count(line, '}');
            loopDepth = Math.max(0, loopDepth - closing);
            boolean loopLine = Pattern.compile("\\b(for|while|do)\\b").matcher(line).find();
            int effectiveDepth = loopDepth + (loopLine ? 1 : 0);

            if (effectiveDepth > 0 && STRING_CONCAT.matcher(line).find()) {
                issues.add(issue(i, "AI Optimization Suggestion",
                        "String concatenation inside a loop can create many temporary objects.",
                        "Use StringBuilder, StringJoiner, or collect values and join once.", line));
            }
            if (effectiveDepth > 0 && REGEX_COMPILE.matcher(line).find()) {
                issues.add(issue(i, "AI Optimization Suggestion",
                        "Regex compilation inside a loop repeats expensive setup work.",
                        "Move Pattern.compile to a static final field or cache compiled patterns.", line));
            }
            if (effectiveDepth > 0 && LINEAR_CONTAINS.matcher(line).find() && !line.contains("HashSet") && !line.contains("Map")) {
                issues.add(issue(i, "AI Optimization Suggestion",
                        "Repeated contains checks inside a loop may become quadratic on lists.",
                        "Pre-index values in a HashSet or HashMap when membership lookup is frequent.", line));
            }
            if (effectiveDepth > 0 && SORT_IN_LOOP.matcher(line).find()) {
                issues.add(issue(i, "AI Optimization Suggestion",
                        "Sorting inside a loop can dominate runtime.",
                        "Sort once before the loop or keep data incrementally ordered only when necessary.", line));
            }

            loopDepth += count(line, '{');
        }
        return issues;
    }

    private CodeIssue issue(int zeroBasedLine, String type, String description, String suggestion, String snippet) {
        return new CodeIssue(zeroBasedLine + 1, type, Severity.MINOR, description, suggestion, snippet.trim());
    }

    private int count(String s, char c) {
        int n = 0;
        for (char ch : s.toCharArray()) if (ch == c) n++;
        return n;
    }
}
