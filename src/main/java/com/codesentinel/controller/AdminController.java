package com.codesentinel.controller;

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
        model.addAttribute("recentUsers", userService.findAll());
        model.addAttribute("activityData", adminService.lastSevenDaysActivity());
        model.addAttribute("activityLabels", String.join(",", adminService.lastSevenDaysActivity().keySet()));
        model.addAttribute("activityValues", adminService.lastSevenDaysActivity().values().stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(""));
        model.addAttribute("activePage", "admin-dashboard");
        return "admin/admin-dashboard";
    }

    @GetMapping("/admin/users")
    public String users(Model model) {
        model.addAttribute("users", adminService.users());
        model.addAttribute("activePage", "admin-users");
        return "admin/user-management";
    }

    @PostMapping("/admin/users/{id}/toggle")
    public String toggle(@PathVariable Long id) {
        userService.toggleActive(id);
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/monitor")
    public String monitor(Model model) {
        model.addAttribute("submissions", adminService.recentSubmissions());
        model.addAttribute("activePage", "admin-monitor");
        return "admin/code-monitor";
    }
}
