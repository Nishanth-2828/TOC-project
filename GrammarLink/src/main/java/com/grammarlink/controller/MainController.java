package com.grammarlink.controller;

import com.grammarlink.dto.ValidationRequest;
import com.grammarlink.dto.ValidationResult;
import com.grammarlink.service.PresetGrammarService;
import com.grammarlink.service.ValidationOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * MainController: Handles page navigation and Thymeleaf template rendering.
 */
@Controller
public class MainController {

    @Autowired private ValidationOrchestrator orchestrator;
    @Autowired private PresetGrammarService presetService;

    /** Landing page */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("presets", presetService.getAllPresets());
        model.addAttribute("request", new ValidationRequest());
        return "index";
    }

    /** Validator page */
    @GetMapping("/validator")
    public String validator(Model model) {
        model.addAttribute("presets", presetService.getAllPresets());
        model.addAttribute("request", new ValidationRequest());
        return "validator";
    }

    /** About page */
    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
