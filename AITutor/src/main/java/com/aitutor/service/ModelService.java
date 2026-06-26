package com.aitutor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModelService {

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<Map<String, String>> getModels(String openAiApiKey) {
        List<Map<String, String>> models = new ArrayList<>();

        try {
            models.addAll(getOllamaModels());
        } catch (Exception e) {
            models.add(Map.of("name", "llama3.2:3b", "provider", "ollama"));
        }

        if (openAiApiKey != null && !openAiApiKey.isBlank()) {
            try {
                models.addAll(getOpenAiModels(openAiApiKey));
            } catch (Exception e) {
                // OpenAI unavailable
            }
        }

        return models;
    }

    private List<Map<String, String>> getOllamaModels() throws Exception {
        String baseUrl = ollamaBaseUrl;
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/tags"))
                .GET()
                .timeout(java.time.Duration.ofSeconds(5))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode modelsNode = root.get("models");

        List<Map<String, String>> models = new ArrayList<>();
        if (modelsNode != null && modelsNode.isArray()) {
            for (JsonNode modelNode : modelsNode) {
                String name = modelNode.get("name").asText();
                Map<String, String> entry = new HashMap<>();
                entry.put("name", name);
                entry.put("provider", "ollama");
                models.add(entry);
            }
        }
        return models;
    }

    private List<Map<String, String>> getOpenAiModels(String apiKey) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/models"))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .timeout(java.time.Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode dataNode = root.get("data");

        List<Map<String, String>> models = new ArrayList<>();
        if (dataNode != null && dataNode.isArray()) {
            for (JsonNode modelNode : dataNode) {
                String id = modelNode.get("id").asText();
                if (id.contains("gpt") || id.contains("o1") || id.contains("o3")) {
                    Map<String, String> entry = new HashMap<>();
                    entry.put("name", id);
                    entry.put("provider", "openai");
                    models.add(entry);
                }
            }
        }
        return models;
    }
}
