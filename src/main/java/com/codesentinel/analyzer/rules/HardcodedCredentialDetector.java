package com.codesentinel.analyzer.rules;

import com.codesentinel.analyzer.model.CodeIssue;
import com.codesentinel.model.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class HardcodedCredentialDetector {
    private static final Pattern SECRET = Pattern.compile("(?i)\\b(password|passwd|pwd|api[_-]?key|secret|token|access[_-]?key)\\b\\s*[:=]\\s*[\"'][^\"']{4,}[\"']");

    public List<CodeIssue> detect(String code) {
        List<CodeIssue> issues = new ArrayList<>();
        String[] lines = code.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            if (SECRET.matcher(lines[i]).find()) {
                issues.add(new CodeIssue(i + 1, "Hardcoded Credential", Severity.CRITICAL,
                        "Possible hardcoded secret or credential found.",
                        "Move secrets into environment variables, a vault, or encrypted configuration.",
                        lines[i].trim()));
            }
        }
        return issues;
    }
}
