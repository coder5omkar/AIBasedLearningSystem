package com.aitutor.service;

import com.aitutor.model.MCQ;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MCQExplanationService {

    private final ChatModelFactory chatModelFactory;
    private final Map<String, Map<Integer, String>> explanationCache = new ConcurrentHashMap<>();

    @Autowired
    public MCQExplanationService(ChatModelFactory chatModelFactory) {
        this.chatModelFactory = chatModelFactory;
    }

    public Map<Integer, String> getExplanations(String sessionId, List<MCQ> mcqs, Map<Integer, String> userAnswers, String provider, String model, String apiKey) {
        ChatModel chatModel = chatModelFactory.createModel(provider, model, apiKey);
        String cacheKey = sessionId + "_" + mcqs.hashCode();

        if (explanationCache.containsKey(cacheKey)) {
            return explanationCache.get(cacheKey);
        }

        Map<Integer, String> explanations = new ConcurrentHashMap<>();

        for (MCQ mcq : mcqs) {
            int questionNumber = mcq.getQuestionNumber();
            String userAnswer = userAnswers.getOrDefault(questionNumber, "Not answered");
            String correctAnswer = mcq.getCorrectAnswer();
            boolean isCorrect = userAnswer.equals(correctAnswer);

            String explanation = generateExplanation(chatModel, mcq, userAnswer, isCorrect);
            explanations.put(questionNumber, explanation);
        }

        explanationCache.put(cacheKey, explanations);

        return explanations;
    }

    private String generateExplanation(ChatModel chatModel, MCQ mcq, String userAnswer, boolean isCorrect) {
        try {
            String promptText = String.format("""
                Please provide a brief explanation for this multiple choice question.
                
                Question: %s
                Options:
                A) %s
                B) %s
                C) %s
                D) %s
                
                Correct Answer: %s
                User's Answer: %s
                User was %s
                
                Provide a clear, educational explanation in 2-3 sentences that explains:
                1. Why the correct answer is correct
                2. If the user was wrong, why their answer was incorrect
                3. Any key concepts related to this question
                
                Keep the explanation concise and educational.
                """,
                    mcq.getQuestion(),
                    mcq.getOptionA(),
                    mcq.getOptionB(),
                    mcq.getOptionC(),
                    mcq.getOptionD(),
                    mcq.getCorrectAnswer(),
                    userAnswer,
                    isCorrect ? "Correct" : "Incorrect"
            );

            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
            messages.add(new UserMessage(promptText));

            Prompt prompt = new Prompt(messages);
            String response = chatModel.call(prompt).getResult().getOutput().getText();

            return response.trim();

        } catch (Exception e) {
            System.err.println("Error generating explanation: " + e.getMessage());
            return generateFallbackExplanation(mcq, userAnswer, isCorrect);
        }
    }

    private String generateFallbackExplanation(MCQ mcq, String userAnswer, boolean isCorrect) {
        StringBuilder explanation = new StringBuilder();
        if (isCorrect) {
            explanation.append("Correct! ");
            explanation.append("The correct answer is ").append(mcq.getCorrectAnswer()).append(". ");
        } else {
            explanation.append("Incorrect. ");
            explanation.append("The correct answer is ").append(mcq.getCorrectAnswer()).append(". ");
            if (!userAnswer.equals("Not answered")) {
                explanation.append("You selected ").append(userAnswer).append(". ");
            }
        }
        explanation.append("Review the topic to better understand the concepts.");
        return explanation.toString();
    }

    public void clearCache(String sessionId) {
        explanationCache.keySet().removeIf(key -> key.startsWith(sessionId));
    }
}
