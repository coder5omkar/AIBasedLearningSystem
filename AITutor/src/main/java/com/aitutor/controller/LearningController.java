package com.aitutor.controller;

import com.aitutor.service.LearningService;
import com.aitutor.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/learning")
public class LearningController {

    private final LearningService learningService;
    private final UserService userService;

    public LearningController(LearningService learningService, UserService userService) {
        this.learningService = learningService;
        this.userService = userService;
    }

    @GetMapping("/progress/{syllabusId}")
    public ResponseEntity<?> getProgress(@PathVariable Long syllabusId, Authentication auth) {
        try {
            Long userId = getUserId(auth);
            return ResponseEntity.ok(learningService.getProgress(userId, syllabusId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mcq/{conceptId}")
    public ResponseEntity<?> getMCQs(
            @PathVariable Long conceptId,
            @RequestParam(defaultValue = "ollama") String provider,
            @RequestParam(defaultValue = "llama3.2:3b") String model,
            @RequestParam(defaultValue = "") String apiKey) {
        try {
            return ResponseEntity.ok(learningService.getConceptMCQs(conceptId, provider, model, apiKey));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/mcq/{conceptId}/submit")
    public ResponseEntity<?> submitMCQs(
            @PathVariable Long conceptId,
            @RequestBody Map<Integer, String> answers,
            Authentication auth) {
        try {
            Long userId = getUserId(auth);
            return ResponseEntity.ok(learningService.submitMCQAnswers(userId, conceptId, answers));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Long getUserId(Authentication auth) {
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        return userService.getUserIdByUsername(userDetails.getUsername());
    }
}
