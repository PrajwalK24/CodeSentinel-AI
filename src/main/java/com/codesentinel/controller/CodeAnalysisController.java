package com.codesentinel.controller;

import com.codesentinel.model.AnalysisReport;
import com.codesentinel.model.User;
import com.codesentinel.analyzer.model.AnalysisResult;
import com.codesentinel.service.CodeAnalysisService;
import com.codesentinel.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CodeAnalysisController {
    private final UserService userService;
    private final CodeAnalysisService codeAnalysisService;

    @GetMapping("/analyze")
    public String analyze(Model model, Authentication auth) {
        model.addAttribute("user", userService.findByEmail(auth.getName()));
        model.addAttribute("activePage", "analyze");
        return "upload";
    }

    @PostMapping("/analyze/file")
    public String analyzeFile(@RequestParam String title,
                              @RequestParam String language,
                              @RequestParam MultipartFile file,
                              Authentication auth,
                              Model model) {
        try {
            User user = userService.findByEmail(auth.getName());
            AnalysisReport report = codeAnalysisService.analyzeFile(user, title, language, file);
            return "redirect:/report/" + report.getId();
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("activePage", "analyze");
            return "upload";
        }
    }

    @PostMapping("/analyze/paste")
    public String analyzePaste(@RequestParam String title,
                               @RequestParam String language,
                               @RequestParam String sourceCode,
                               Authentication auth) {
        User user = userService.findByEmail(auth.getName());
        AnalysisReport report = codeAnalysisService.analyzePaste(user, title, language, sourceCode);
        return "redirect:/report/" + report.getId();
    }

    @PostMapping("/analyze/live")
    @ResponseBody
    public AnalysisResult analyzeLive(@RequestBody Map<String, String> request) {
        return codeAnalysisService.analyzeLive(request.getOrDefault("sourceCode", ""));
    }
}
