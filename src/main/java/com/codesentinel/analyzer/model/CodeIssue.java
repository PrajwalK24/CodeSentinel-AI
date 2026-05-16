package com.codesentinel.analyzer.model;

import com.codesentinel.model.Severity;
import lombok.Getter;

@Getter
public class CodeIssue {
    private final int lineNumber;
    private final String issueType;
    private final Severity severity;
    private final String description;
    private final String suggestion;
    private final String codeSnippet;
    private final String impact;
    private final String confidence;

    public CodeIssue(int lineNumber, String issueType, Severity severity, String description, String suggestion, String codeSnippet) {
        this(lineNumber, issueType, severity, description, suggestion, codeSnippet, "High Confidence");
    }

    public CodeIssue(int lineNumber, String issueType, Severity severity, String description, String suggestion, String codeSnippet, String confidence) {
        this(lineNumber, issueType, severity, description, suggestion, codeSnippet, defaultImpact(severity), confidence);
    }

    public CodeIssue(int lineNumber, String issueType, Severity severity, String description, String suggestion, String codeSnippet, String impact, String confidence) {
        this.lineNumber = lineNumber;
        this.issueType = issueType;
        this.severity = severity;
        this.description = description;
        this.suggestion = suggestion;
        this.codeSnippet = codeSnippet;
        this.impact = impact;
        this.confidence = confidence;
    }

    private static String defaultImpact(Severity severity) {
        return switch (severity) {
            case CRITICAL -> "Can crash at runtime, expose sensitive data, or allow direct exploitation.";
            case MAJOR -> "Can produce wrong output, non-termination, security weakness, or serious runtime failure.";
            case MINOR -> "Can fail on edge cases or create avoidable performance/resource cost.";
            case INFO -> "Low-risk improvement with limited runtime impact.";
        };
    }
}
