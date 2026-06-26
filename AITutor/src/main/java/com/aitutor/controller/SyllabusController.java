package com.aitutor.controller;

import com.aitutor.model.Concept;
import com.aitutor.model.Syllabus;
import com.aitutor.service.SyllabusService;
import com.aitutor.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/syllabus")
public class SyllabusController {

    private final SyllabusService syllabusService;
    private final UserService userService;

    public SyllabusController(SyllabusService syllabusService, UserService userService) {
        this.syllabusService = syllabusService;
        this.userService = userService;
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveSyllabus(Authentication auth) {
        try {
            Long userId = getUserId(auth);
            Syllabus syllabus = syllabusService.getActiveSyllabus(userId);
            if (syllabus == null) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(Map.of("message", "No active syllabus"));
            }
            return ResponseEntity.ok(syllabus);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSyllabus(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Long userId = getUserId(auth);
            String title = (String) body.get("title");
            String content = (String) body.get("content");
            Syllabus syllabus = syllabusService.updateSyllabus(id, userId, title, content);
            return ResponseEntity.ok(syllabus);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateSyllabus(
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long userId = getUserId(auth);
            if (syllabusService.hasActiveSyllabus(userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "You already have an active syllabus. Complete it before starting a new subject."));
            }
            Long subjectId = Long.valueOf(body.get("subjectId").toString());
            String provider = (String) body.getOrDefault("provider", "ollama");
            String model = (String) body.getOrDefault("model", "llama3.2:3b");
            String apiKey = (String) body.getOrDefault("apiKey", "");

            Syllabus syllabus = syllabusService.generateSyllabus(subjectId, userId, provider, model, apiKey);
            return ResponseEntity.ok(syllabus);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadSyllabus(
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long userId = getUserId(auth);
            if (syllabusService.hasActiveSyllabus(userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "You already have an active syllabus. Complete it before starting a new subject."));
            }
            Long subjectId = Long.valueOf(body.get("subjectId").toString());
            String title = (String) body.get("title");
            String content = (String) body.get("content");

            Syllabus syllabus = syllabusService.uploadSyllabus(subjectId, userId, title, content);
            return ResponseEntity.ok(syllabus);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/structure")
    public ResponseEntity<?> getStructure(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = getUserId(auth);
            return ResponseEntity.ok(syllabusService.getSyllabusStructure(id, userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserSyllabi(Authentication auth) {
        try {
            Long userId = getUserId(auth);
            return ResponseEntity.ok(syllabusService.getUserSyllabi(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/concept/{conceptId}/regenerate")
    public ResponseEntity<?> regenerateConceptContent(
            @PathVariable Long conceptId,
            @RequestBody Map<String, Object> body) {
        try {
            boolean simplified = Boolean.parseBoolean(body.getOrDefault("simplified", "false").toString());
            String provider = (String) body.getOrDefault("provider", "ollama");
            String model = (String) body.getOrDefault("model", "llama3.2:3b");
            String apiKey = (String) body.getOrDefault("apiKey", "");
            Concept concept = syllabusService.regenerateConceptContent(conceptId, simplified, provider, model, apiKey);
            return ResponseEntity.ok(concept);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/concept/{conceptId}/content")
    public ResponseEntity<?> getConceptContent(
            @PathVariable Long conceptId,
            @RequestParam(defaultValue = "ollama") String provider,
            @RequestParam(defaultValue = "llama3.2:3b") String model,
            @RequestParam(defaultValue = "") String apiKey) {
        try {
            Concept concept = syllabusService.getConceptWithContent(conceptId, provider, model, apiKey);
            return ResponseEntity.ok(concept);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Long getUserId(Authentication auth) {
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        return userService.getUserIdByUsername(userDetails.getUsername());
    }
}
