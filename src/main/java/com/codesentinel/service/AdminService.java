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

    public long submissionsToday() {
        return submissionRepository.countBySubmittedAtAfter(LocalDateTime.now().toLocalDate().atStartOfDay());
    }
}
