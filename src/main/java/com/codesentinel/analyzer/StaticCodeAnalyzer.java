package com.codesentinel.analyzer;

import com.codesentinel.analyzer.model.AnalysisResult;
import com.codesentinel.analyzer.model.CodeIssue;
import com.codesentinel.analyzer.rules.*;
import com.codesentinel.model.RiskLevel;
import com.codesentinel.model.Severity;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class StaticCodeAnalyzer {
    private final InfiniteLoopDetector infiniteLoopDetector = new InfiniteLoopDetector();
    private final NullPointerDetector nullPointerDetector = new NullPointerDetector();
    private final HardcodedCredentialDetector hardcodedCredentialDetector = new HardcodedCredentialDetector();
    private final SecurityVulnerabilityDetector securityVulnerabilityDetector = new SecurityVulnerabilityDetector();
    private final ArithmeticErrorDetector arithmeticErrorDetector = new ArithmeticErrorDetector();
    private final LogicalRuntimeBugDetector logicalRuntimeBugDetector = new LogicalRuntimeBugDetector();
    private final SemanticIndexDetector semanticIndexDetector = new SemanticIndexDetector();
    private final ResourceConcurrencyDetector resourceConcurrencyDetector = new ResourceConcurrencyDetector();
    private final ComplexityCalculator complexityCalculator = new ComplexityCalculator();

    public AnalysisResult analyze(String code) {
        long start = System.currentTimeMillis();
        AnalysisResult result = new AnalysisResult();
        result.getIssues().addAll(hardcodedCredentialDetector.detect(code));
        result.getIssues().addAll(securityVulnerabilityDetector.detect(code));
        result.getIssues().addAll(arithmeticErrorDetector.detect(code));
        result.getIssues().addAll(logicalRuntimeBugDetector.detect(code));
        result.getIssues().addAll(semanticIndexDetector.detect(code));
        result.getIssues().addAll(resourceConcurrencyDetector.detect(code));
        result.getIssues().addAll(infiniteLoopDetector.detect(code));
        result.getIssues().addAll(nullPointerDetector.detect(code));
        result.getIssues().sort(Comparator.comparingInt(CodeIssue::getLineNumber));
        int complexity = complexityCalculator.calculate(code);
        result.setComplexityScore(complexity);
        result.setTimeComplexity(complexityCalculator.estimateTimeComplexity(code));
        result.setSpaceComplexity(complexityCalculator.estimateSpaceComplexity(code));
        result.setRiskLevel(overallRisk(result, complexityCalculator.riskFor(complexity)));
        result.setDurationMs(System.currentTimeMillis() - start);
        return result;
    }

    private RiskLevel overallRisk(AnalysisResult result, RiskLevel complexityRisk) {
        long critical = result.getIssues().stream().filter(i -> i.getSeverity() == Severity.CRITICAL).count();
        long major = result.getIssues().stream().filter(i -> i.getSeverity() == Severity.MAJOR).count();
        if (critical > 1 || complexityRisk == RiskLevel.CRITICAL) return RiskLevel.CRITICAL;
        if (critical == 1 || major >= 4 || complexityRisk == RiskLevel.HIGH) return RiskLevel.HIGH;
        if (major > 0 || complexityRisk == RiskLevel.MEDIUM) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }
}
