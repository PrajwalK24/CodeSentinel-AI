package com.codesentinel.analyzer.rules;

import com.codesentinel.analyzer.model.CodeIssue;
import com.codesentinel.model.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class HardcodedCredentialDetector {
    private static final Pattern SECRET = Pattern.compile("(?i)\\b(password|passwd|pwd|api[_-]?key|secret|token|access[_-]?key|client[_-]?secret|auth[_-]?token|bearer)\\b\\s*[:=]\\s*[\"'][^\"']{4,}[\"']");
    private static final Pattern USERNAME = Pattern.compile("(?i)\\b(user(name)?|login)\\b\\s*[:=]\\s*[\"'][^\"']{3,}[\"']");
    private static final Pattern PRIVATE_URL = Pattern.compile("(?i)[\"']https?://[^\"']*(internal|private|admin|token|key|secret|password)[^\"']*[\"']");
    private static final Pattern CLOUD_KEY = Pattern.compile("\\b(AKIA|ASIA)[A-Z0-9]{16}\\b");
    private static final Pattern PRIVATE_KEY = Pattern.compile("-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----");

    public List<CodeIssue> detect(String code) {
        List<CodeIssue> issues = new ArrayList<>();
        String[] lines = CodeSanitizer.stripComments(code).split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            if (SECRET.matcher(lines[i]).find() || CLOUD_KEY.matcher(lines[i]).find() || PRIVATE_KEY.matcher(lines[i]).find()) {
                issues.add(new CodeIssue(i + 1, "Hardcoded Credential", Severity.CRITICAL,
                        "A secret, token, API key, password, cloud key, or private key is embedded directly in source code.",
                        "Replace the literal with an environment variable or vault lookup, for example: String token = System.getenv(\"API_TOKEN\");",
                        lines[i].trim(),
                        "Leaked source code or logs can expose reusable credentials and allow account or system compromise.",
                        "High Confidence"));
            } else if (USERNAME.matcher(lines[i]).find() || PRIVATE_URL.matcher(lines[i]).find()) {
                issues.add(new CodeIssue(i + 1, "Hardcoded Sensitive Data", Severity.MAJOR,
                        "A username/login value or private URL is embedded directly in source code.",
                        "Load sensitive identifiers and internal URLs from configuration, for example: String user = config.getUsername();",
                        lines[i].trim(),
                        "Exposed identifiers and internal endpoints make credential attacks and environment discovery easier.",
                        "Medium Confidence"));
            }
        }
        return issues;
    }
}
