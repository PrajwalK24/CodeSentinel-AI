package com.codesentinel.controller;

import com.codesentinel.model.AnalysisReport;
import com.codesentinel.model.User;
import com.codesentinel.service.ReportService;
import com.codesentinel.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    private final UserService userService;

    @GetMapping("/report/{id}")
    public String report(@PathVariable Long id, Authentication auth, Model model) {
        AnalysisReport report = reportService.findReport(id);
        model.addAttribute("report", report);
        model.addAttribute("timeComplexity", report.getTimeComplexity() == null || report.getTimeComplexity().isBlank() ? "O(1)" : report.getTimeComplexity());
        model.addAttribute("spaceComplexity", report.getSpaceComplexity() == null || report.getSpaceComplexity().isBlank() ? "O(1)" : report.getSpaceComplexity());
        model.addAttribute("codeLines", report.getSubmission().getSourceCode().split("\\R", -1));
        Set<Integer> issueLines = report.getIssues().stream().map(i -> i.getLineNumber()).collect(Collectors.toSet());
        model.addAttribute("issueLines", issueLines);
        model.addAttribute("user", userService.findByEmail(auth.getName()));
        model.addAttribute("activePage", "history");
        return "report";
    }

    @GetMapping("/history")
    public String history(Authentication auth, Model model) {
        User user = userService.findByEmail(auth.getName());
        model.addAttribute("user", user);
        model.addAttribute("reports", reportService.historyFor(user));
        model.addAttribute("activePage", "history");
        return "history";
    }
}
