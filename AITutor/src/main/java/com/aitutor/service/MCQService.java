package com.aitutor.service;

import com.aitutor.model.MCQ;
import com.aitutor.repository.MCQRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MCQService {

    private final ChatModelFactory chatModelFactory;
    private final MCQRepository mcqRepository;
    private final MemoryService memoryService;

    @Autowired
    public MCQService(ChatModelFactory chatModelFactory, MCQRepository mcqRepository, MemoryService memoryService) {
        this.chatModelFactory = chatModelFactory;
        this.mcqRepository = mcqRepository;
        this.memoryService = memoryService;
    }

    @Transactional
    public List<MCQ> generateMCQsFromConversation(Long userId, String sessionId, String provider, String model, String apiKey) {
        ChatModel chatModel = chatModelFactory.createModel(provider, model, apiKey);

        mcqRepository.deleteByUserIdAndSessionId(userId, sessionId);

        List<Message> history = memoryService.getMessages(userId, sessionId, 20);

        String conversationContext = extractConversationContext(history);

        if (conversationContext.isEmpty()) {
            conversationContext = "general knowledge";
        }

        String promptText = String.format("""
            Based on the following conversation, generate 5 multiple choice questions that test understanding of the topics discussed.
            
            Conversation:
            %s
            
            IMPORTANT FORMATTING RULES:
            1. Each question MUST be on a new line starting with "Q1:", "Q2:", etc.
            2. Each option MUST be on a new line starting with "A)", "B)", "C)", "D)"
            3. The answer MUST be on a new line starting with "Answer: "
            4. Do not use any special characters or markdown formatting
            5. Keep questions and options concise and complete
            
            Example format:
            Q1: What is the capital of France?
            A) London
            B) Paris
            C) Berlin
            D) Madrid
            Answer: B
            
            Generate 5 questions now:
            """, conversationContext);

        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(promptText));

        Prompt prompt = new Prompt(messages);
        String response = chatModel.call(prompt).getResult().getOutput().getText();

        System.out.println("Raw MCQ Response: " + response);

        List<MCQ> mcqs = parseMCQsFromResponse(response, sessionId);

        if (mcqs.isEmpty()) {
            mcqs = generateFallbackMCQs(sessionId);
        }

        mcqs.forEach(mcq -> mcq.setUserId(userId));
        mcqs = mcqRepository.saveAll(mcqs);

        return mcqs;
    }

    private List<MCQ> generateFallbackMCQs(String sessionId) {
        List<MCQ> fallbackMCQs = new ArrayList<>();

        String[] fallbackQuestions = {
                "What is the main topic discussed in the conversation?",
                "What key concept was explained in the conversation?",
                "What question did the user ask about the topic?",
                "What response did the assistant provide?",
                "What additional information was shared?"
        };

        String[][] fallbackOptions = {
                {"The conversation topic", "Unrelated topic", "Random topic", "No topic"},
                {"Key concept 1", "Key concept 2", "Key concept 3", "Key concept 4"},
                {"Question about topic", "Question about weather", "Question about food", "Question about sports"},
                {"Helpful response", "Unhelpful response", "Irrelevant response", "No response"},
                {"Additional details", "No details", "Wrong details", "Confusing details"}
        };

        String[] fallbackAnswers = {"A", "A", "A", "A", "A"};

        for (int i = 0; i < 5; i++) {
            MCQ mcq = MCQ.builder()
                    .sessionId(sessionId)
                    .question(fallbackQuestions[i])
                    .optionA(fallbackOptions[i][0])
                    .optionB(fallbackOptions[i][1])
                    .optionC(fallbackOptions[i][2])
                    .optionD(fallbackOptions[i][3])
                    .correctAnswer(fallbackAnswers[i])
                    .questionNumber(i + 1)
                    .createdAt(LocalDateTime.now())
                    .build();
            fallbackMCQs.add(mcq);
        }

        return fallbackMCQs;
    }

    private String extractConversationContext(List<Message> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        StringBuilder context = new StringBuilder();
        for (Message msg : history) {
            String role = msg.getMessageType().name().toLowerCase();
            String content = msg.getText();
            content = content.replaceAll("[^\\w\\s.,?!-]", " ");
            context.append(role).append(": ").append(content).append("\n");
        }
        return context.toString();
    }

    private List<MCQ> parseMCQsFromResponse(String response, String sessionId) {
        List<MCQ> mcqs = new ArrayList<>();
        response = response.replaceAll("\\*\\*", "");
        response = response.replaceAll("```", "");

        Pattern questionPattern = Pattern.compile("Q(\\d+):\\s*([^\\n]+)");
        Matcher questionMatcher = questionPattern.matcher(response);

        List<String> questionBlocks = new ArrayList<>();
        int lastEnd = 0;
        while (questionMatcher.find()) {
            int start = questionMatcher.start();
            if (lastEnd > 0) {
                String block = response.substring(lastEnd, start).trim();
                if (!block.isEmpty()) {
                    questionBlocks.add(block);
                }
            }
            lastEnd = start;
        }
        if (lastEnd < response.length()) {
            String block = response.substring(lastEnd).trim();
            if (!block.isEmpty()) {
                questionBlocks.add(block);
            }
        }

        for (String block : questionBlocks) {
            MCQ mcq = parseSingleQuestion(block, sessionId);
            if (mcq != null) {
                mcqs.add(mcq);
            }
        }

        if (mcqs.size() < 5) {
            mcqs = parseAlternativeFormat(response, sessionId);
        }

        return mcqs;
    }

    private MCQ parseSingleQuestion(String block, String sessionId) {
        try {
            Pattern numPattern = Pattern.compile("Q(\\d+)");
            Matcher numMatcher = numPattern.matcher(block);
            int questionNumber = 1;
            if (numMatcher.find()) {
                questionNumber = Integer.parseInt(numMatcher.group(1));
            }

            Pattern questionTextPattern = Pattern.compile("Q\\d+:\\s*([^\\n]+)");
            Matcher questionTextMatcher = questionTextPattern.matcher(block);
            String question = "";
            if (questionTextMatcher.find()) {
                question = questionTextMatcher.group(1).trim();
            }

            Pattern optionPattern = Pattern.compile("([A-D])\\)\\s*([^\\n]+)");
            Matcher optionMatcher = optionPattern.matcher(block);

            String optionA = "", optionB = "", optionC = "", optionD = "";
            String correctAnswer = "";

            while (optionMatcher.find()) {
                String optionLetter = optionMatcher.group(1);
                String optionText = optionMatcher.group(2).trim();
                switch (optionLetter) {
                    case "A": optionA = optionText; break;
                    case "B": optionB = optionText; break;
                    case "C": optionC = optionText; break;
                    case "D": optionD = optionText; break;
                }
            }

            Pattern answerPattern = Pattern.compile("Answer:\\s*([A-D])", Pattern.CASE_INSENSITIVE);
            Matcher answerMatcher = answerPattern.matcher(block);
            if (answerMatcher.find()) {
                correctAnswer = answerMatcher.group(1).toUpperCase();
            }

            if (!question.isEmpty() && !optionA.isEmpty()) {
                return MCQ.builder()
                        .sessionId(sessionId)
                        .question(question)
                        .optionA(optionA)
                        .optionB(optionB)
                        .optionC(optionC)
                        .optionD(optionD)
                        .correctAnswer(correctAnswer)
                        .questionNumber(questionNumber)
                        .createdAt(LocalDateTime.now())
                        .build();
            }
        } catch (Exception e) {
            System.err.println("Error parsing question: " + e.getMessage());
        }
        return null;
    }

    private List<MCQ> parseAlternativeFormat(String response, String sessionId) {
        List<MCQ> mcqs = new ArrayList<>();
        String[] lines = response.split("\n");
        String currentQuestion = "";
        String currentOptions = "";
        String currentAnswer = "";
        int questionNumber = 1;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.matches("(?i)^q\\d+[:.]\\s*.*")) {
                if (!currentQuestion.isEmpty()) {
                    MCQ mcq = createMCQFromParts(currentQuestion, currentOptions, currentAnswer, sessionId, questionNumber++);
                    if (mcq != null) {
                        mcqs.add(mcq);
                    }
                }
                currentQuestion = line.replaceFirst("(?i)^q\\d+[:.]\\s*", "").trim();
                currentOptions = "";
                currentAnswer = "";
            } else if (line.matches("(?i)^answer[:.]\\s*[A-D]")) {
                currentAnswer = line.replaceFirst("(?i)^answer[:.]\\s*", "").trim().toUpperCase();
            } else if (line.matches("(?i)^[A-D][).]\\s*.*")) {
                currentOptions += line + "\n";
            } else if (!currentQuestion.isEmpty()) {
                currentQuestion += " " + line;
            }
        }

        if (!currentQuestion.isEmpty()) {
            MCQ mcq = createMCQFromParts(currentQuestion, currentOptions, currentAnswer, sessionId, questionNumber);
            if (mcq != null) {
                mcqs.add(mcq);
            }
        }

        return mcqs;
    }

    private MCQ createMCQFromParts(String question, String options, String answer, String sessionId, int questionNumber) {
        try {
            String optionA = "", optionB = "", optionC = "", optionD = "";
            Pattern optionPattern = Pattern.compile("([A-D])[).]\\s*([^\\n]+)");
            Matcher optionMatcher = optionPattern.matcher(options);

            while (optionMatcher.find()) {
                String optionLetter = optionMatcher.group(1);
                String optionText = optionMatcher.group(2).trim();
                switch (optionLetter) {
                    case "A": optionA = optionText; break;
                    case "B": optionB = optionText; break;
                    case "C": optionC = optionText; break;
                    case "D": optionD = optionText; break;
                }
            }

            if (!question.isEmpty() && !optionA.isEmpty()) {
                return MCQ.builder()
                        .sessionId(sessionId)
                        .question(question)
                        .optionA(optionA)
                        .optionB(optionB)
                        .optionC(optionC)
                        .optionD(optionD)
                        .correctAnswer(answer)
                        .questionNumber(questionNumber)
                        .createdAt(LocalDateTime.now())
                        .build();
            }
        } catch (Exception e) {
            System.err.println("Error creating MCQ from parts: " + e.getMessage());
        }
        return null;
    }

    public List<MCQ> getMCQsBySession(Long userId, String sessionId) {
        return mcqRepository.findByUserIdAndSessionIdOrderByQuestionNumberAsc(userId, sessionId);
    }
}
