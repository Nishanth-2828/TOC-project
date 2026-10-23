package com.grammarlink.controller;

import com.grammarlink.dto.ValidationRequest;
import com.grammarlink.dto.ValidationResult;
import com.grammarlink.service.PresetGrammarService;
import com.grammarlink.service.ValidationOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * ApiController: REST endpoints for AJAX-based validation calls.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired private ValidationOrchestrator orchestrator;
    @Autowired private PresetGrammarService presetService;

    /**
     * POST /api/validate
     * Accepts a grammar + test string, runs full validation pipeline,
     * returns JSON result.
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validate(@RequestBody ValidationRequest request) {
        ValidationResult result = orchestrator.validate(request);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/presets
     * Returns all available preset grammars.
     */
    @GetMapping("/presets")
    public ResponseEntity<?> getPresets() {
        return ResponseEntity.ok(presetService.getAllPresets());
    }

    /**
     * GET /api/presets/{id}
     * Returns a specific preset grammar by ID.
     */
    @GetMapping("/presets/{id}")
    public ResponseEntity<?> getPreset(@PathVariable String id) {
        try {
            return ResponseEntity.ok(presetService.getPreset(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/health
     * Simple health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "OK");
        status.put("app", "GrammarLink CFG–PDA Validator");
        status.put("version", "1.0.0");
        return ResponseEntity.ok(status);
    }
}
