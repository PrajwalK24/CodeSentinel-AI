package com.codesentinel.service;

import com.codesentinel.analyzer.StaticCodeAnalyzer;
import com.codesentinel.analyzer.model.AnalysisResult;
import com.codesentinel.analyzer.model.CodeIssue;
import com.codesentinel.model.*;
import com.codesentinel.repository.AnalysisReportRepository;
import com.codesentinel.repository.CodeSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CodeAnalysisService {
    private final CodeSubmissionRepository submissionRepository;
    private final AnalysisReportRepository reportRepository;
    private final StaticCodeAnalyzer analyzer;

    @Transactional
    public AnalysisReport analyzePaste(User user, String title, String language, String code) {
        return createReport(user, title, language, code, SubmissionType.PASTE, null);
    }

    @Transactional
    public AnalysisReport analyzeFile(User user, String title, String language, MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename() == null ? "uploaded-code.txt" : file.getOriginalFilename();
        String code = new String(file.getBytes(), StandardCharsets.UTF_8);
        return createReport(user, title.isBlank() ? fileName : title, language, code, SubmissionType.FILE, fileName);
    }

    private AnalysisReport createReport(User user, String title, String language, String code, SubmissionType type, String fileName) {
        CodeSubmission submission = new CodeSubmission();
        submission.setUser(user);
        submission.setTitle(title);
        submission.setLanguage(language);
        submission.setSourceCode(code);
        submission.setSubmissionType(type);
        submission.setFileName(fileName);
        submissionRepository.save(submission);

        AnalysisResult result = analyzer.analyze(code);
        AnalysisReport report = new AnalysisReport();
        report.setSubmission(submission);
        report.setTotalBugs(result.getIssues().size());
        report.setCriticalCount((int) result.criticalCount());
        report.setMajorCount((int) result.majorCount());
        report.setMinorCount((int) result.minorCount());
        report.setComplexityScore(result.getComplexityScore());
        report.setTimeComplexity(result.getTimeComplexity());
        report.setSpaceComplexity(result.getSpaceComplexity());
        report.setRiskLevel(result.getRiskLevel());
        report.setAnalysisDurationMs(result.getDurationMs());
        for (CodeIssue issue : result.getIssues()) {
            BugIssue entity = new BugIssue();
            entity.setReport(report);
            entity.setLineNumber(issue.getLineNumber());
            entity.setIssueType(issue.getIssueType());
            entity.setSeverity(issue.getSeverity());
            entity.setDescription(issue.getDescription());
            entity.setSuggestion(issue.getSuggestion());
            entity.setImpact(issue.getImpact());
            entity.setConfidence(issue.getConfidence());
            entity.setCodeSnippet(issue.getCodeSnippet());
            report.getIssues().add(entity);
        }
        return reportRepository.save(report);
    }

    public List<CodeSubmission> recentFor(User user) {
        return submissionRepository.findByUserWithReport(user).stream().limit(8).toList();
    }

    public List<CodeSubmission> allRecent() {
        return submissionRepository.findAllWithUserAndReport().stream().limit(20).toList();
    }

    public AnalysisResult analyzeLive(String code) {
        return analyzer.analyze(code == null ? "" : code);
    }

    public long countAll() {
        return submissionRepository.count();
    }

    public long countFor(User user) {
        return submissionRepository.countByUser(user);
    }
}
