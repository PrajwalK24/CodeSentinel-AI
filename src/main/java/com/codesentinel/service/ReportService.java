package com.codesentinel.service;

import com.codesentinel.model.AnalysisReport;
import com.codesentinel.model.User;
import com.codesentinel.repository.AnalysisReportRepository;
import com.codesentinel.repository.BugIssueRepository;
import com.codesentinel.repository.CodeSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final AnalysisReportRepository reportRepository;
    private final BugIssueRepository issueRepository;
    private final CodeSubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public AnalysisReport findReport(Long id) {
        AnalysisReport report = reportRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Report not found"));
        report.getIssues().size();
        report.getSubmission().getTitle();
        return report;
    }

    public List<AnalysisReport> historyFor(User user) {
        return reportRepository.findByUser(user);
    }

    public long totalBugsFor(User user) {
        return reportRepository.sumBugsByUser(user);
    }

    public long criticalFor(User user) {
        return reportRepository.sumCriticalByUser(user);
    }

    public double avgComplexityFor(User user) {
        return reportRepository.avgComplexityByUser(user);
    }

    public Map<String, Long> severityDistribution(User user) {
        Map<String, Long> data = new LinkedHashMap<>();
        data.put("CRITICAL", 0L);
        data.put("MAJOR", 0L);
        data.put("MINOR", 0L);
        data.put("INFO", 0L);
        for (Object[] row : issueRepository.severityDistributionByUser(user)) {
            data.put(String.valueOf(row[0]), (Long) row[1]);
        }
        return data;
    }

    public Map<String, Long> languageDistribution() {
        Map<String, Long> data = new LinkedHashMap<>();
        for (Object[] row : submissionRepository.countByLanguage()) {
            data.put(String.valueOf(row[0]), (Long) row[1]);
        }
        return data;
    }

    public long allBugs() {
        return reportRepository.sumAllBugs();
    }
}
