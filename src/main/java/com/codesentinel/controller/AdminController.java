package com.codesentinel.controller;

import com.codesentinel.model.Role;
import com.codesentinel.service.AdminService;
import com.codesentinel.service.CodeAnalysisService;
import com.codesentinel.service.ReportService;
import com.codesentinel.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;
    private final CodeAnalysisService codeAnalysisService;
    private final ReportService reportService;
    private final AdminService adminService;

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userService.countAll());
        model.addAttribute("activeUsers", userService.countActive());
        model.addAttribute("totalSubmissions", codeAnalysisService.countAll());
        model.addAttribute("totalBugs", reportService.allBugs());
        model.addAttribute("totalCritical", reportService.allCritical());
        model.addAttribute("avgComplexity", String.format("%.1f", reportService.platformAvgComplexity()));
        model.addAttribute("recentUsers", userService.findAll());
        model.addAttribute("roleSummary", adminService.userRoleSummary());
        model.addAttribute("platformRisk", adminService.riskDistribution());
        model.addAttribute("platformSeverity", reportService.platformSeverityDistribution());
        model.addAttribute("topIssueTypes", reportService.platformIssueTypeDistribution());
        model.addAttribute("highRiskSubmissions", adminService.highRiskSubmissions());
        model.addAttribute("submissionsToday", adminService.submissionsToday());
        var activityData = adminService.lastSevenDaysActivity();
        model.addAttribute("activityData", activityData);
        model.addAttribute("activityLabels", String.join(",", activityData.keySet()));
        model.addAttribute("activityValues", activityData.values().stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(""));
        model.addAttribute("activePage", "admin-dashboard");
        return "admin/admin-dashboard";
    }

    @GetMapping("/admin/users")
    public String users(Model model) {
        model.addAttribute("users", adminService.users());
        model.addAttribute("roleSummary", adminService.userRoleSummary());
        model.addAttribute("activeUsers", userService.countActive());
        model.addAttribute("totalUsers", userService.countAll());
        model.addAttribute("activePage", "admin-users");
        return "admin/user-management";
    }

    @PostMapping("/admin/users/{id}/toggle")
    public String toggle(@PathVariable Long id) {
        userService.toggleActive(id);
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/role")
    public String updateRole(@PathVariable Long id, @RequestParam Role role) {
        userService.updateRole(id, role);
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/monitor")
    public String monitor(Model model) {
        model.addAttribute("submissions", adminService.recentSubmissions());
        model.addAttribute("platformRisk", adminService.riskDistribution());
        model.addAttribute("languageData", reportService.languageDistribution());
        model.addAttribute("activePage", "admin-monitor");
        return "admin/code-monitor";
    }
}
