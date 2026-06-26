package com.aitutor.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatModelFactory {

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    public ChatModel createModel(String provider, String model, String apiKey) {
        if ("openai".equalsIgnoreCase(provider)) {
            return OpenAiChatModel.builder()
                    .options(OpenAiChatOptions.builder()
                            .model(model != null ? model : "gpt-4o")
                            .apiKey(apiKey != null ? apiKey : "")
                            .build())
                    .build();
        }
        return OllamaChatModel.builder()
                .ollamaApi(OllamaApi.builder()
                        .baseUrl(ollamaBaseUrl)
                        .build())
                .options(OllamaChatOptions.builder()
                        .model(model != null ? model : "llama3.2:3b")
                        .build())
                .build();
    }
}
