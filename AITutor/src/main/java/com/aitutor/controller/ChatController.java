package com.aitutor.controller;

import com.aitutor.dto.ChatRequest;
import com.aitutor.service.ChatService;
import com.aitutor.service.MemoryService;
import com.aitutor.service.UserService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final MemoryService memoryService;
    private final UserService userService;

    @Autowired
    public ChatController(ChatService chatService, MemoryService memoryService, UserService userService) {
        this.chatService = chatService;
        this.memoryService = memoryService;
        this.userService = userService;
    }

    @PostMapping
    public Map<String, String> chat(@RequestParam String sessionId, @RequestBody ChatRequest request, Authentication auth) {
        Long userId = userService.getUserIdByUsername(auth.getName());
        String response = chatService.chat(userId, sessionId, request.getMessage(), request.getProvider(), request.getModel(), request.getApiKey());
        return Map.of("response", response);
    }

    @PostMapping("/ask-doubt")
    public Map<String, Object> askDoubt(@RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = userService.getUserIdByUsername(auth.getName());
        Long conceptId = Long.valueOf(body.get("conceptId").toString());
        String question = (String) body.get("question");
        String provider = (String) body.getOrDefault("provider", "ollama");
        String model = (String) body.getOrDefault("model", "llama3.2:3b");
        String apiKey = (String) body.getOrDefault("apiKey", "");

        String response = chatService.askDoubt(userId, conceptId, question, provider, model, apiKey);
        return Map.of("response", response);
    }

    @PostMapping("/with-topic")
    public Map<String, String> chatWithTopic(
            @RequestParam String sessionId,
            @RequestParam String topic,
            @RequestBody ChatRequest request,
            Authentication auth) {
        Long userId = userService.getUserIdByUsername(auth.getName());
        String response = chatService.chatWithTopic(userId, sessionId, request.getMessage(), topic, request.getProvider(), request.getModel(), request.getApiKey());
        return Map.of("response", response);
    }

    @GetMapping("/history")
    public List<Message> history(@RequestParam String sessionId, Authentication auth) {
        Long userId = userService.getUserIdByUsername(auth.getName());
        return chatService.getHistory(userId, sessionId);
    }

    @GetMapping("/sessions")
    public List<String> getSessions(Authentication auth) {
        Long userId = userService.getUserIdByUsername(auth.getName());
        return memoryService.getAllSessions(userId);
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<?> deleteSession(@PathVariable String sessionId, Authentication auth) {
        try {
            Long userId = userService.getUserIdByUsername(auth.getName());
            memoryService.clear(userId, sessionId);
            return ResponseEntity.ok(Map.of("message", "Session deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to delete session: " + e.getMessage()));
        }
    }
}
