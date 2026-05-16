package com.codesentinel.analyzer.rules;

import com.codesentinel.analyzer.model.CodeIssue;
import com.codesentinel.model.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ArithmeticErrorDetector {
    private static final Pattern DIVIDE_BY_ZERO = Pattern.compile("(?<!/)(?:/|%)\\s*(?:0|0\\.0+)\\b");

    public List<CodeIssue> detect(String code) {
        List<CodeIssue> issues = new ArrayList<>();
        String[] originalLines = code.split("\\R", -1);
        String[] cleanLines = CodeSanitizer.stripCommentsAndStrings(code).split("\\R", -1);
        for (int i = 0; i < cleanLines.length; i++) {
            if (DIVIDE_BY_ZERO.matcher(cleanLines[i]).find()) {
                issues.add(new CodeIssue(i + 1, "Division By Zero", Severity.CRITICAL,
                        "A number is divided or moduloed by zero, which will fail at runtime.",
                        "Use a non-zero denominator or guard the denominator before the operation.",
                        originalLines[i].trim()));
            }
        }
        return issues;
    }
}
