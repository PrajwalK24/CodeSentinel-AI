package com.codesentinel.analyzer.model;

import com.codesentinel.model.RiskLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AnalysisResult {
    private final List<CodeIssue> issues = new ArrayList<>();
    private int complexityScore;
    private String timeComplexity = "O(1)";
    private String spaceComplexity = "O(1)";
    private RiskLevel riskLevel = RiskLevel.LOW;
    private long durationMs;

    public long criticalCount() {
        return issues.stream().filter(i -> i.getSeverity().name().equals("CRITICAL")).count();
    }

    public long majorCount() {
        return issues.stream().filter(i -> i.getSeverity().name().equals("MAJOR")).count();
    }

    public long minorCount() {
        return issues.stream().filter(i -> i.getSeverity().name().equals("MINOR")).count();
    }
}
