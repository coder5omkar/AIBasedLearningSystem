package com.aitutor.controller;

import com.aitutor.dto.ChatRequest;
import com.aitutor.model.MCQ;
import com.aitutor.service.MCQExplanationService;
import com.aitutor.service.MCQService;
import com.aitutor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mcq")
public class MCQController {

    private final MCQService mcqService;
    private final MCQExplanationService explanationService;
    private final UserService userService;

    @Autowired
    public MCQController(MCQService mcqService, MCQExplanationService explanationService, UserService userService) {
        this.mcqService = mcqService;
        this.explanationService = explanationService;
        this.userService = userService;
    }

    @PostMapping("/generate")
    public List<MCQ> generateMCQs(@RequestParam String sessionId, @RequestBody(required = false) ChatRequest request, Authentication auth) {
        Long userId = userService.getUserIdByUsername(auth.getName());
        String provider = request != null ? request.getProvider() : null;
        String model = request != null ? request.getModel() : null;
        String apiKey = request != null ? request.getApiKey() : null;
        return mcqService.generateMCQsFromConversation(userId, sessionId, provider, model, apiKey);
    }

    @GetMapping("/session/{sessionId}")
    public List<MCQ> getMCQsBySession(@PathVariable String sessionId, Authentication auth) {
        Long userId = userService.getUserIdByUsername(auth.getName());
        return mcqService.getMCQsBySession(userId, sessionId);
    }

    @PostMapping("/explanations")
    public Map<Integer, String> getExplanations(
            @RequestParam String sessionId,
            @RequestBody Map<Integer, String> userAnswers,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String apiKey,
            Authentication auth) {
        Long userId = userService.getUserIdByUsername(auth.getName());
        List<MCQ> mcqs = mcqService.getMCQsBySession(userId, sessionId);
        return explanationService.getExplanations(sessionId, mcqs, userAnswers, provider, model, apiKey);
    }
}
