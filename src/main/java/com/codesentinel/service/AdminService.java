package com.codesentinel.service;

import com.codesentinel.model.AnalysisReport;
import com.codesentinel.model.CodeSubmission;
import com.codesentinel.model.User;
import com.codesentinel.repository.AnalysisReportRepository;
import com.codesentinel.repository.CodeSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserService userService;
    private final CodeSubmissionRepository submissionRepository;
    private final AnalysisReportRepository reportRepository;

    public List<User> users() {
        return userService.findAll();
    }

    public List<CodeSubmission> recentSubmissions() {
        return submissionRepository.findAllWithUserAndReport().stream().limit(20).toList();
    }

    public Map<String, Long> lastSevenDaysActivity() {
        LocalDate start = LocalDate.now().minusDays(6);
        Map<String, Long> data = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            data.put(start.plusDays(i).toString(), 0L);
        }
        List<AnalysisReport> reports = reportRepository.findCreatedAfter(start.atStartOfDay());
        for (AnalysisReport report : reports) {
            String key = report.getCreatedAt().toLocalDate().toString();
            data.computeIfPresent(key, (k, v) -> v + 1);
        }
        return data;
    }

    public Map<String, Long> riskDistribution() {
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("LOW", 0L);
        distribution.put("MEDIUM", 0L);
        distribution.put("HIGH", 0L);
        distribution.put("CRITICAL", 0L);
        for (Object[] row : reportRepository.riskDistribution()) {
            distribution.put(String.valueOf(row[0]), (Long) row[1]);
        }
        return distribution;
    }

    public List<CodeSubmission> highRiskSubmissions() {
        return submissionRepository.findAllWithUserAndReport().stream()
                .filter(s -> s.getReport() != null && (s.getReport().getRiskLevel() == com.codesentinel.model.RiskLevel.HIGH || s.getReport().getRiskLevel() == com.codesentinel.model.RiskLevel.CRITICAL))
                .limit(20)
                .toList();
    }

    public Map<String, Long> userRoleSummary() {
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("ADMIN", userService.countByRole(com.codesentinel.model.Role.ADMIN));
        summary.put("DEVELOPER", userService.countByRole(com.codesentinel.model.Role.DEVELOPER));
        return summary;
    }

    public long submissionsToday() {
        return submissionRepository.countBySubmittedAtAfter(LocalDateTime.now().toLocalDate().atStartOfDay());
    }
}
