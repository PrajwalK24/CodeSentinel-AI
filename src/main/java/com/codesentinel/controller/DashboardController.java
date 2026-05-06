package com.codesentinel.controller;

import com.codesentinel.model.User;
import com.codesentinel.service.CodeAnalysisService;
import com.codesentinel.service.ReportService;
import com.codesentinel.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final UserService userService;
    private final CodeAnalysisService codeAnalysisService;
    private final ReportService reportService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User user = userService.findByEmail(auth.getName());
        model.addAttribute("user", user);
        model.addAttribute("recentSubmissions", codeAnalysisService.recentFor(user));
        model.addAttribute("totalAnalyses", codeAnalysisService.countFor(user));
        model.addAttribute("totalBugs", reportService.totalBugsFor(user));
        model.addAttribute("criticalIssues", reportService.criticalFor(user));
        model.addAttribute("avgComplexity", String.format("%.1f", reportService.avgComplexityFor(user)));
        model.addAttribute("severityData", reportService.severityDistribution(user));
        model.addAttribute("languageData", reportService.languageDistribution());
        model.addAttribute("languageLabels", String.join(",", reportService.languageDistribution().keySet()));
        model.addAttribute("languageValues", reportService.languageDistribution().values().stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(""));
        model.addAttribute("activePage", "dashboard");
        return "dashboard";
    }
}
