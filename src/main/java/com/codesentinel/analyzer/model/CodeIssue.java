package com.codesentinel.analyzer.model;

import com.codesentinel.model.Severity;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CodeIssue {
    private final int lineNumber;
    private final String issueType;
    private final Severity severity;
    private final String description;
    private final String suggestion;
    private final String codeSnippet;
}
