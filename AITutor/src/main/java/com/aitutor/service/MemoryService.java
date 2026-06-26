package com.aitutor.service;

import com.aitutor.dto.ChatMessage;
import com.aitutor.model.ChatMessageEntity;
import com.aitutor.repository.ChatMessageRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MemoryService {

    private final ChatMessageRepository repository;

    public MemoryService(ChatMessageRepository repository) {
        this.repository = repository;
    }

    public void addMessages(Long userId, String sessionId, List<Message> messages) {
        messages.forEach(msg -> {
            if (msg instanceof SystemMessage) {
                return;
            }
            ChatMessageEntity entity = ChatMessageEntity.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .role(msg.getMessageType().name().toLowerCase())
                    .content(msg.getText())
                    .timestamp(LocalDateTime.now())
                    .build();
            repository.save(entity);
        });
    }

    public List<Message> getMessages(Long userId, String sessionId, int lastN) {
        List<ChatMessageEntity> entities = repository.findByUserIdAndSessionIdOrderByTimestampAsc(userId, sessionId);
        List<Message> messages = new ArrayList<>();

        int startIndex = Math.max(0, entities.size() - lastN);
        for (int i = startIndex; i < entities.size(); i++) {
            ChatMessageEntity entity = entities.get(i);
            Message msg = switch (entity.getRole().toLowerCase()) {
                case "user" -> new UserMessage(entity.getContent());
                case "assistant" -> new AssistantMessage(entity.getContent());
                case "system" -> new SystemMessage(entity.getContent());
                default -> throw new IllegalArgumentException("Unknown role: " + entity.getRole());
            };
            messages.add(msg);
        }
        return messages;
    }

    public List<ChatMessage> getChatMessages(Long userId, String sessionId, int lastN) {
        List<ChatMessageEntity> entities = repository.findByUserIdAndSessionIdOrderByTimestampAsc(userId, sessionId);
        List<ChatMessage> messages = new ArrayList<>();

        int startIndex = Math.max(0, entities.size() - lastN);
        for (int i = startIndex; i < entities.size(); i++) {
            ChatMessageEntity entity = entities.get(i);
            if (!"system".equalsIgnoreCase(entity.getRole())) {
                messages.add(new ChatMessage(entity.getRole(), entity.getContent()));
            }
        }
        return messages;
    }

    public List<String> getAllSessions(Long userId) {
        return repository.findDistinctSessionIdsByUserId(userId);
    }

    public void clear(Long userId, String sessionId) {
        repository.deleteByUserIdAndSessionId(userId, sessionId);
    }
}
