package com.aitutor.service;

import com.aitutor.model.Concept;
import com.aitutor.repository.ConceptRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private final ChatModelFactory chatModelFactory;
    private final MemoryService memoryService;
    private final SystemPromptService systemPromptService;
    private final ConceptRepository conceptRepository;

    @Autowired
    public ChatService(ChatModelFactory chatModelFactory, MemoryService memoryService,
                       SystemPromptService systemPromptService, ConceptRepository conceptRepository) {
        this.chatModelFactory = chatModelFactory;
        this.memoryService = memoryService;
        this.systemPromptService = systemPromptService;
        this.conceptRepository = conceptRepository;
    }

    public String chat(Long userId, String sessionId, String userMessage, String provider, String model, String apiKey) {
        ChatModel chatModel = chatModelFactory.createModel(provider, model, apiKey);

        String systemPrompt = systemPromptService.getSystemPrompt();
        List<Message> history = memoryService.getMessages(userId, sessionId, 10);

        List<Message> allMessages = new ArrayList<>();
        allMessages.add(new SystemMessage(systemPrompt));
        allMessages.addAll(history);
        allMessages.add(new UserMessage(userMessage));

        Prompt prompt = new Prompt(allMessages);
        String response = chatModel.call(prompt).getResult().getOutput().getText();

        memoryService.addMessages(userId, sessionId, List.of(
                new UserMessage(userMessage),
                new AssistantMessage(response)
        ));

        return response;
    }

    public String chatWithTopic(Long userId, String sessionId, String userMessage, String topic, String provider, String model, String apiKey) {
        ChatModel chatModel = chatModelFactory.createModel(provider, model, apiKey);

        String systemPrompt = systemPromptService.getSystemPromptForTopic(topic);
        List<Message> history = memoryService.getMessages(userId, sessionId, 10);

        List<Message> allMessages = new ArrayList<>();
        allMessages.add(new SystemMessage(systemPrompt));
        allMessages.addAll(history);
        allMessages.add(new UserMessage(userMessage));

        Prompt prompt = new Prompt(allMessages);
        String response = chatModel.call(prompt).getResult().getOutput().getText();

        memoryService.addMessages(userId, sessionId, List.of(
                new UserMessage(userMessage),
                new AssistantMessage(response)
        ));

        return response;
    }

    public String askDoubt(Long userId, Long conceptId, String question, String provider, String model, String apiKey) {
        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> new RuntimeException("Concept not found"));

        ChatModel chatModel = chatModelFactory.createModel(provider, model, apiKey);
        String sessionId = "doubt-" + conceptId;

        String systemPrompt = "You are a tutor helping a student understand the concept: '" + concept.getTitle() + "'.\n\n"
                + "Concept content:\n" + (concept.getContent() != null ? concept.getContent() : "Content not yet loaded.")
                + "\n\nAnswer the student's question about this concept clearly and helpfully."
                + " Use analogies, examples, and simple language. If the question is outside this concept, gently guide back.";

        List<Message> history = memoryService.getMessages(userId, sessionId, 10);
        List<Message> allMessages = new ArrayList<>();
        allMessages.add(new SystemMessage(systemPrompt));
        allMessages.addAll(history);
        allMessages.add(new UserMessage(question));

        Prompt prompt = new Prompt(allMessages);
        String response = chatModel.call(prompt).getResult().getOutput().getText();

        memoryService.addMessages(userId, sessionId, List.of(
                new UserMessage(question),
                new AssistantMessage(response)
        ));

        return response;
    }

    public List<Message> getHistory(Long userId, String sessionId) {
        return memoryService.getMessages(userId, sessionId, 100);
    }
}
