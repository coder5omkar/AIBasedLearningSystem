package com.aitutor.service;

import com.aitutor.model.*;
import com.aitutor.repository.*;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LearningService {

    private final ConceptRepository conceptRepository;
    private final UserProgressRepository userProgressRepository;
    private final ChapterRepository chapterRepository;
    private final SectionRepository sectionRepository;
    private final SyllabusRepository syllabusRepository;
    private final ChatModelFactory chatModelFactory;
    private final MCQRepository mcqRepository;

    public LearningService(ConceptRepository conceptRepository,
                           UserProgressRepository userProgressRepository,
                           ChapterRepository chapterRepository,
                           SectionRepository sectionRepository,
                           SyllabusRepository syllabusRepository,
                           ChatModelFactory chatModelFactory,
                           MCQRepository mcqRepository) {
        this.conceptRepository = conceptRepository;
        this.userProgressRepository = userProgressRepository;
        this.chapterRepository = chapterRepository;
        this.sectionRepository = sectionRepository;
        this.syllabusRepository = syllabusRepository;
        this.chatModelFactory = chatModelFactory;
        this.mcqRepository = mcqRepository;
    }

    public List<Map<String, Object>> getProgress(Long userId, Long syllabusId) {
        List<Chapter> chapters = chapterRepository.findBySyllabusIdOrderByChapterOrderAsc(syllabusId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Chapter chapter : chapters) {
            Map<String, Object> chapterMap = new HashMap<>();
            chapterMap.put("id", chapter.getId());
            chapterMap.put("title", chapter.getTitle());
            chapterMap.put("order", chapter.getChapterOrder());

            List<Section> sections = sectionRepository.findByChapterIdOrderBySectionOrderAsc(chapter.getId());
            List<Map<String, Object>> sectionList = new ArrayList<>();
            int totalConcepts = 0;
            int completedConcepts = 0;

            for (Section section : sections) {
                Map<String, Object> sectionMap = new HashMap<>();
                sectionMap.put("id", section.getId());
                sectionMap.put("title", section.getTitle());

                List<Concept> concepts = conceptRepository.findBySectionIdOrderByConceptOrderAsc(section.getId());
                List<Map<String, Object>> conceptList = new ArrayList<>();
                for (Concept concept : concepts) {
                    totalConcepts++;
                    UserProgress progress = userProgressRepository
                            .findByUserIdAndConceptId(userId, concept.getId())
                            .orElse(null);

                    Map<String, Object> cm = new HashMap<>();
                    cm.put("id", concept.getId());
                    cm.put("title", concept.getTitle());
                    cm.put("order", concept.getConceptOrder());

                    String status = "locked";
                    if (progress != null) {
                        status = progress.getStatus();
                        if ("completed".equals(status)) completedConcepts++;
                    } else {
                        Long previousConceptId = getPreviousConceptId(concept.getId(), sections, concepts);
                        if (previousConceptId == null) {
                            status = "available";
                        } else {
                            UserProgress prevProgress = userProgressRepository
                                    .findByUserIdAndConceptId(userId, previousConceptId)
                                    .orElse(null);
                            if (prevProgress != null && "completed".equals(prevProgress.getStatus())) {
                                status = "available";
                            }
                        }
                    }
                    cm.put("status", status);
                    conceptList.add(cm);
                }
                sectionMap.put("concepts", conceptList);
                sectionList.add(sectionMap);
            }
            chapterMap.put("sections", sectionList);
            chapterMap.put("totalConcepts", totalConcepts);
            chapterMap.put("completedConcepts", completedConcepts);
            result.add(chapterMap);
        }
        return result;
    }

    private Long getPreviousConceptId(Long currentConceptId, List<Section> sections, List<Concept> currentSectionConcepts) {
        for (int i = 0; i < currentSectionConcepts.size(); i++) {
            if (currentSectionConcepts.get(i).getId().equals(currentConceptId) && i > 0) {
                return currentSectionConcepts.get(i - 1).getId();
            }
        }
        if (currentSectionConcepts.isEmpty() || !currentSectionConcepts.get(0).getId().equals(currentConceptId)) {
            return null;
        }
        return null;
    }

    public List<MCQ> getConceptMCQs(Long conceptId, String provider, String model, String apiKey) {
        List<MCQ> existing = mcqRepository.findByConceptId(conceptId);
        if (!existing.isEmpty()) {
            boolean hasSource = existing.stream().allMatch(m -> m.getSourceSnippet() != null && !m.getSourceSnippet().isEmpty());
            if (hasSource) {
                return existing;
            }
            mcqRepository.deleteAll(existing);
        }
        return generateConceptMCQs(conceptId, provider, model, apiKey);
    }

    private List<MCQ> generateConceptMCQs(Long conceptId, String provider, String model, String apiKey) {
        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> new RuntimeException("Concept not found"));

        String studyContent = concept.getContent();
        String contentBasedPrompt;
        if (studyContent != null && !studyContent.trim().isEmpty()) {
            contentBasedPrompt = "Generate 5 multiple-choice questions based STRICTLY on the following study material.\n"
                    + "Each question must be answerable from the material provided — do NOT test outside knowledge.\n"
                    + "After each answer, include a \"Source:\" line with a brief verbatim or near-verbatim excerpt\n"
                    + "from the study material that supports the correct answer.\n\n"
                    + "--- STUDY MATERIAL ---\n"
                    + studyContent + "\n"
                    + "--- END OF STUDY MATERIAL ---\n\n"
                    + "Format EXACTLY like this:\n"
                    + "Q1: [Question text]\n"
                    + "A) [Option A]\n"
                    + "B) [Option B]\n"
                    + "C) [Option C]\n"
                    + "D) [Option D]\n"
                    + "Answer: [A/B/C/D]\n"
                    + "Source: [A short excerpt from the study material that supports the answer]\n\n"
                    + "Q2: ... and so on for all 5 questions.";
        } else {
            contentBasedPrompt = "Generate 5 multiple-choice questions about '" + concept.getTitle() + "'.\n"
                    + "For each question, provide 4 options (A, B, C, D) and indicate the correct answer.\n"
                    + "Each question should test understanding, not just memorization.\n\n"
                    + "Format EXACTLY like this:\n"
                    + "Q1: [Question text]\n"
                    + "A) [Option A]\n"
                    + "B) [Option B]\n"
                    + "C) [Option C]\n"
                    + "D) [Option D]\n"
                    + "Answer: [A/B/C/D]\n\n"
                    + "Q2: ... and so on for all 5 questions.";
        }

        try {
            var chatModel = chatModelFactory.createModel(provider, model, apiKey);
            List<Message> messages = List.of(
                    new SystemMessage("You are an educational assessment creator."),
                    new UserMessage(contentBasedPrompt)
            );
            Prompt p = new Prompt(messages);
            String content = chatModel.call(p).getResult().getOutput().getText();
            return parseAndSaveMCQs(content, conceptId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate MCQs: " + e.getMessage());
        }
    }

    private List<MCQ> parseAndSaveMCQs(String response, Long conceptId) {
        List<MCQ> mcqs = new ArrayList<>();
        String[] blocks = response.split("(?=Q\\d+:)");
        int qNum = 1;

        for (String block : blocks) {
            if (!block.matches("(?s).*Q\\d+:.*")) continue;

            Map<String, String> options = new LinkedHashMap<>();
            String question = "";
            String correctAnswer = "";
            String sourceSnippet = "";

            String[] lines = block.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.matches("Q\\d+:.*")) {
                    question = line.replaceFirst("Q\\d+:", "").trim();
                } else if (line.matches("[Aa]\\)\\s*.*")) {
                    options.put("A", line.replaceFirst("[Aa]\\)\\s*", "").trim());
                } else if (line.matches("[Bb]\\)\\s*.*")) {
                    options.put("B", line.replaceFirst("[Bb]\\)\\s*", "").trim());
                } else if (line.matches("[Cc]\\)\\s*.*")) {
                    options.put("C", line.replaceFirst("[Cc]\\)\\s*", "").trim());
                } else if (line.matches("[Dd]\\)\\s*.*")) {
                    options.put("D", line.replaceFirst("[Dd]\\)\\s*", "").trim());
                } else if (line.matches("(?i)Answer:\\s*[A-Da-d].*")) {
                    correctAnswer = line.replaceFirst("(?i)Answer:\\s*", "").trim().toUpperCase();
                    if (correctAnswer.length() > 1) correctAnswer = correctAnswer.substring(0, 1);
                } else if (line.matches("(?i)Source:.*")) {
                    sourceSnippet = line.replaceFirst("(?i)Source:\\s*", "").trim();
                }
            }

            if (!question.isEmpty() && options.size() == 4 && !correctAnswer.isEmpty()) {
                MCQ mcq = MCQ.builder()
                        .conceptId(conceptId)
                        .sessionId("concept-" + conceptId)
                        .question(question)
                        .optionA(options.get("A"))
                        .optionB(options.get("B"))
                        .optionC(options.get("C"))
                        .optionD(options.get("D"))
                        .correctAnswer(correctAnswer)
                        .sourceSnippet(sourceSnippet)
                        .questionNumber(qNum++)
                        .createdAt(LocalDateTime.now())
                        .build();
                mcqs.add(mcqRepository.save(mcq));
            }
        }

        if (mcqs.isEmpty()) {
            for (int i = 0; i < 5; i++) {
                MCQ mcq = MCQ.builder()
                        .conceptId(conceptId)
                        .sessionId("concept-" + conceptId)
                        .question("Sample question " + (i + 1))
                        .optionA("Option A")
                        .optionB("Option B")
                        .optionC("Option C")
                        .optionD("Option D")
                        .correctAnswer("A")
                        .sourceSnippet("")
                        .questionNumber(i + 1)
                        .createdAt(LocalDateTime.now())
                        .build();
                mcqs.add(mcqRepository.save(mcq));
            }
        }
        return mcqs;
    }

    @Transactional
    public Map<String, Object> submitMCQAnswers(Long userId, Long conceptId, Map<Integer, String> answers) {
        List<MCQ> mcqs = mcqRepository.findByConceptId(conceptId);
        int correct = 0;
        int total = mcqs.size();

        List<Map<String, Object>> results = new ArrayList<>();
        for (MCQ mcq : mcqs) {
            String userAnswer = answers.get(mcq.getQuestionNumber());
            boolean isCorrect = userAnswer != null && userAnswer.equalsIgnoreCase(mcq.getCorrectAnswer());
            if (isCorrect) correct++;

            Map<String, Object> r = new HashMap<>();
            r.put("questionNumber", mcq.getQuestionNumber());
            r.put("question", mcq.getQuestion());
            r.put("userAnswer", userAnswer);
            r.put("correctAnswer", mcq.getCorrectAnswer());
            r.put("isCorrect", isCorrect);
            r.put("sourceSnippet", mcq.getSourceSnippet() != null ? mcq.getSourceSnippet() : "");
            results.add(r);
        }

        UserProgress progress = userProgressRepository
                .findByUserIdAndConceptId(userId, conceptId)
                .orElse(UserProgress.builder()
                        .userId(userId)
                        .conceptId(conceptId)
                        .status("in_progress")
                        .mcqAttempts(0)
                        .mcqCorrect(0)
                        .totalMcqQuestions(total)
                        .mcqPassed(false)
                        .updatedAt(LocalDateTime.now())
                        .build());

        progress.setMcqAttempts(progress.getMcqAttempts() + 1);
        progress.setMcqCorrect(correct);
        progress.setMcqPassed(correct == total);
        if (correct == total) {
            progress.setStatus("completed");
            progress.setCompletedAt(LocalDateTime.now());
        } else {
            progress.setStatus("in_progress");
        }
        progress.setUpdatedAt(LocalDateTime.now());
        userProgressRepository.save(progress);

        Map<String, Object> result = new HashMap<>();
        result.put("correct", correct);
        result.put("total", total);
        result.put("passed", correct == total);
        result.put("attempts", progress.getMcqAttempts());
        result.put("results", results);
        return result;
    }
}
