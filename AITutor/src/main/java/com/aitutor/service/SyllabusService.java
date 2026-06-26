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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SyllabusService {

    private final SyllabusRepository syllabusRepository;
    private final ChapterRepository chapterRepository;
    private final SectionRepository sectionRepository;
    private final ConceptRepository conceptRepository;
    private final SubjectRepository subjectRepository;
    private final UserProgressRepository userProgressRepository;
    private final ChatModelFactory chatModelFactory;

    public SyllabusService(SyllabusRepository syllabusRepository,
                           ChapterRepository chapterRepository,
                           SectionRepository sectionRepository,
                           ConceptRepository conceptRepository,
                           SubjectRepository subjectRepository,
                           UserProgressRepository userProgressRepository,
                           ChatModelFactory chatModelFactory) {
        this.syllabusRepository = syllabusRepository;
        this.chapterRepository = chapterRepository;
        this.sectionRepository = sectionRepository;
        this.conceptRepository = conceptRepository;
        this.subjectRepository = subjectRepository;
        this.userProgressRepository = userProgressRepository;
        this.chatModelFactory = chatModelFactory;
    }

    @Transactional
    public Syllabus generateSyllabus(Long subjectId, Long userId, String provider, String model, String apiKey) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        String prompt = buildSyllabusPrompt(subject);
        String response = callLlm(prompt, provider, model, apiKey);

        Syllabus syllabus = Syllabus.builder()
                .subjectId(subjectId)
                .userId(userId)
                .title(subject.getName() + " Syllabus")
                .generatedBy("llm")
                .rawContent(response)
                .createdAt(LocalDateTime.now())
                .build();
        syllabus = syllabusRepository.save(syllabus);

        parseAndSaveStructure(syllabus.getId(), response);

        return syllabus;
    }

    @Transactional
    public Syllabus uploadSyllabus(Long subjectId, Long userId, String title, String content) {
        Syllabus syllabus = Syllabus.builder()
                .subjectId(subjectId)
                .userId(userId)
                .title(title)
                .generatedBy("uploaded")
                .rawContent(content)
                .createdAt(LocalDateTime.now())
                .build();
        syllabus = syllabusRepository.save(syllabus);

        parseAndSaveStructure(syllabus.getId(), content);

        return syllabus;
    }

    private String buildSyllabusPrompt(Subject subject) {
        return "You are a curriculum designer. Create a detailed syllabus for the subject: \""
                + subject.getName() + "\".\n\n"
                + "The syllabus must follow this EXACT structure:\n\n"
                + "Chapter 1: [Chapter Title]\n"
                + "  Section 1.1: [Section Title]\n"
                + "    Concept: [Concept 1 Title]\n"
                + "    Concept: [Concept 2 Title]\n"
                + "    Concept: [Concept 3 Title]\n"
                + "  Section 1.2: [Section Title]\n"
                + "    Concept: [Concept Title]\n"
                + "    Concept: [Concept Title]\n\n"
                + "Chapter 2: [Chapter Title]\n"
                + "  Section 2.1: [Section Title]\n"
                + "    Concept: [Concept Title]\n\n"
                + "Requirements:\n"
                + "- Create 3-5 chapters\n"
                + "- Each chapter has 2-3 sections\n"
                + "- Each section has 2-4 concepts\n"
                + "- Each concept should be a single, focused topic\n"
                + "- Use the EXACT format shown above with 'Chapter', 'Section', and 'Concept:' prefixes\n"
                + "- Do not add any extra text before or after the syllabus";
    }

    private String callLlm(String prompt, String provider, String model, String apiKey) {
        try {
            var chatModel = chatModelFactory.createModel(provider, model, apiKey);
            List<Message> messages = List.of(
                    new SystemMessage("You are a curriculum designer. Respond ONLY with the syllabus, no extra text."),
                    new UserMessage(prompt)
            );
            Prompt p = new Prompt(messages);
            return chatModel.call(p).getResult().getOutput().getText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate syllabus: " + e.getMessage());
        }
    }

    private void parseAndSaveStructure(Long syllabusId, String content) {
        Map<Integer, ChapterData> chapters = new LinkedHashMap<>();
        Pattern chapterPattern = Pattern.compile("Chapter\\s+(\\d+):\\s*(.*?)(?=Chapter\\s+\\d+:|$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher chapterMatcher = chapterPattern.matcher(content);

        while (chapterMatcher.find()) {
            int chapterNum = Integer.parseInt(chapterMatcher.group(1).trim());
            String chapterBody = chapterMatcher.group(0);
            String chapterTitle = extractChapterTitle(chapterMatcher.group(2));

            ChapterData chapterData = new ChapterData(chapterTitle, new LinkedHashMap<>());

            Pattern sectionPattern = Pattern.compile("Section\\s+" + chapterNum + "\\.(\\d+):\\s*(.*?)(?=Section\\s+" + chapterNum + "\\.\\d+:|Chapter\\s+\\d+:|$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher sectionMatcher = sectionPattern.matcher(chapterBody);

            while (sectionMatcher.find()) {
                int sectionNum = Integer.parseInt(sectionMatcher.group(1).trim());
                String sectionBody = sectionMatcher.group(0);
                String sectionTitle = sectionMatcher.group(2).replaceAll("(?s)\\s*Concept:.*", "").trim();

                List<String> concepts = new ArrayList<>();
                Pattern conceptPattern = Pattern.compile("Concept:\\s*(.*?)(?=Concept:|Section\\s+|Chapter\\s+|$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                Matcher conceptMatcher = conceptPattern.matcher(sectionBody);

                while (conceptMatcher.find()) {
                    String conceptTitle = conceptMatcher.group(1).trim();
                    if (!conceptTitle.isEmpty()) {
                        concepts.add(conceptTitle);
                    }
                }

                chapterData.sections.put(sectionNum, new SectionData(sectionTitle, concepts));
            }

            chapters.put(chapterNum, chapterData);
        }

        if (chapters.isEmpty()) {
            String[] lines = content.split("\n");
            int chapterNum = 0;
            int sectionNum = 0;
            ChapterData currentChapter = null;
            List<String> currentConcepts = new ArrayList<>();
            String currentSectionTitle = "";

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                Matcher chMatcher = Pattern.compile("Chapter\\s+(\\d+)", Pattern.CASE_INSENSITIVE).matcher(line);
                if (chMatcher.find()) {
                    if (currentChapter != null && !currentSectionTitle.isEmpty()) {
                        currentChapter.sections.put(++sectionNum, new SectionData(currentSectionTitle, new ArrayList<>(currentConcepts)));
                        currentConcepts.clear();
                    }
                    chapterNum = Integer.parseInt(chMatcher.group(1));
                    String title = line.replaceFirst("Chapter\\s+\\d+[:\\.]?\\s*", "").trim();
                    currentChapter = new ChapterData(title.isEmpty() ? "Chapter " + chapterNum : title, new LinkedHashMap<>());
                    chapters.put(chapterNum, currentChapter);
                    sectionNum = 0;
                    currentSectionTitle = "";
                    continue;
                }

                Matcher secMatcher = Pattern.compile("Section\\s+\\d+\\.(\\d+)", Pattern.CASE_INSENSITIVE).matcher(line);
                if (secMatcher.find() && currentChapter != null) {
                    if (!currentSectionTitle.isEmpty()) {
                        currentChapter.sections.put(++sectionNum, new SectionData(currentSectionTitle, new ArrayList<>(currentConcepts)));
                        currentConcepts.clear();
                    }
                    sectionNum = Integer.parseInt(secMatcher.group(1));
                    currentSectionTitle = line.replaceFirst("Section\\s+\\d+\\.\\d+[:\\.]?\\s*", "").trim();
                    if (currentSectionTitle.isEmpty()) currentSectionTitle = "Section " + sectionNum;
                    continue;
                }

                Matcher conMatcher = Pattern.compile("Concept[:\\.]?", Pattern.CASE_INSENSITIVE).matcher(line);
                if (conMatcher.find() && currentChapter != null) {
                    String conceptTitle = line.replaceFirst("Concept[:\\.]?\\s*", "").trim();
                    if (!conceptTitle.isEmpty()) {
                        currentConcepts.add(conceptTitle);
                    }
                }
            }
            if (currentChapter != null && !currentSectionTitle.isEmpty()) {
                currentChapter.sections.put(++sectionNum, new SectionData(currentSectionTitle, new ArrayList<>(currentConcepts)));
            }
        }

        for (Map.Entry<Integer, ChapterData> chEntry : chapters.entrySet()) {
            Chapter chapter = Chapter.builder()
                    .syllabusId(syllabusId)
                    .title(chEntry.getValue().title)
                    .chapterOrder(chEntry.getKey())
                    .build();
            chapter = chapterRepository.save(chapter);

            for (Map.Entry<Integer, SectionData> secEntry : chEntry.getValue().sections.entrySet()) {
                Section section = Section.builder()
                        .chapterId(chapter.getId())
                        .title(secEntry.getValue().title)
                        .sectionOrder(secEntry.getKey())
                        .build();
                section = sectionRepository.save(section);

                int conceptOrder = 1;
                for (String conceptTitle : secEntry.getValue().concepts) {
                    Concept concept = Concept.builder()
                            .sectionId(section.getId())
                            .title(conceptTitle)
                            .content("")
                            .conceptOrder(conceptOrder++)
                            .build();
                    conceptRepository.save(concept);
                }
            }
        }
    }

    private String extractChapterTitle(String raw) {
        String[] lines = raw.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.matches("(?i)section\\s+\\d+.*") && !line.matches("(?i)concept.*")) {
                return line;
            }
        }
        return raw.trim();
    }

    public Map<String, Object> getSyllabusStructure(Long syllabusId, Long userId) {
        Syllabus syllabus = syllabusRepository.findByIdAndUserId(syllabusId, userId)
                .orElseThrow(() -> new RuntimeException("Syllabus not found"));

        List<Chapter> chapters = chapterRepository.findBySyllabusIdOrderByChapterOrderAsc(syllabusId);

        List<Map<String, Object>> chapterList = new ArrayList<>();
        for (Chapter chapter : chapters) {
            Map<String, Object> chapterMap = new HashMap<>();
            chapterMap.put("id", chapter.getId());
            chapterMap.put("title", chapter.getTitle());
            chapterMap.put("order", chapter.getChapterOrder());

            List<Section> sections = sectionRepository.findByChapterIdOrderBySectionOrderAsc(chapter.getId());
            List<Map<String, Object>> sectionList = new ArrayList<>();
            for (Section section : sections) {
                Map<String, Object> sectionMap = new HashMap<>();
                sectionMap.put("id", section.getId());
                sectionMap.put("title", section.getTitle());
                sectionMap.put("order", section.getSectionOrder());

                List<Concept> concepts = conceptRepository.findBySectionIdOrderByConceptOrderAsc(section.getId());
                sectionMap.put("concepts", concepts.stream().map(c -> {
                    Map<String, Object> cm = new HashMap<>();
                    cm.put("id", c.getId());
                    cm.put("title", c.getTitle());
                    cm.put("order", c.getConceptOrder());
                    return cm;
                }).collect(Collectors.toList()));

                sectionList.add(sectionMap);
            }
            chapterMap.put("sections", sectionList);
            chapterList.add(chapterMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", syllabus.getId());
        result.put("title", syllabus.getTitle());
        result.put("subjectId", syllabus.getSubjectId());
        result.put("generatedBy", syllabus.getGeneratedBy());
        result.put("createdAt", syllabus.getCreatedAt());
        result.put("chapters", chapterList);

        return result;
    }

    public List<Map<String, Object>> getUserSyllabi(Long userId) {
        return syllabusRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("title", s.getTitle());
            m.put("subjectId", s.getSubjectId());
            m.put("generatedBy", s.getGeneratedBy());
            m.put("createdAt", s.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
    }

    public Syllabus getActiveSyllabus(Long userId) {
        List<Syllabus> syllabi = syllabusRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (Syllabus s : syllabi) {
            List<Chapter> chapters = chapterRepository.findBySyllabusIdOrderByChapterOrderAsc(s.getId());
            for (Chapter ch : chapters) {
                List<Section> sections = sectionRepository.findByChapterIdOrderBySectionOrderAsc(ch.getId());
                for (Section sec : sections) {
                    List<Concept> concepts = conceptRepository.findBySectionIdOrderByConceptOrderAsc(sec.getId());
                    for (Concept c : concepts) {
                        UserProgress p = userProgressRepository.findByUserIdAndConceptId(userId, c.getId()).orElse(null);
                        if (p == null || !"completed".equals(p.getStatus())) {
                            return s;
                        }
                    }
                }
            }
        }
        return null;
    }

    public boolean hasActiveSyllabus(Long userId) {
        return getActiveSyllabus(userId) != null;
    }

    @Transactional
    public Syllabus updateSyllabus(Long syllabusId, Long userId, String title, String content) {
        Syllabus syllabus = syllabusRepository.findByIdAndUserId(syllabusId, userId)
                .orElseThrow(() -> new RuntimeException("Syllabus not found"));
        if (title != null) syllabus.setTitle(title);
        if (content != null) {
            syllabus.setRawContent(content);
            syllabus.setUpdatedAt(LocalDateTime.now());
            deleteSyllabusStructure(syllabusId);
            parseAndSaveStructure(syllabusId, content);
        }
        return syllabusRepository.save(syllabus);
    }

    private void deleteSyllabusStructure(Long syllabusId) {
        List<Chapter> chapters = chapterRepository.findBySyllabusIdOrderByChapterOrderAsc(syllabusId);
        for (Chapter ch : chapters) {
            List<Section> sections = sectionRepository.findByChapterIdOrderBySectionOrderAsc(ch.getId());
            for (Section sec : sections) {
                conceptRepository.deleteBySectionId(sec.getId());
            }
            sectionRepository.deleteByChapterId(ch.getId());
        }
        chapterRepository.deleteBySyllabusId(syllabusId);
    }

    public Concept getConceptWithContent(Long conceptId, String provider, String model, String apiKey) {
        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> new RuntimeException("Concept not found"));

        if (concept.getContent() == null || concept.getContent().isEmpty()) {
            String content = generateConceptContent(concept, provider, model, apiKey);
            concept.setContent(content);
            conceptRepository.save(concept);
        }
        return concept;
    }

    private String generateConceptContent(Concept concept, String provider, String model, String apiKey) {
        return callLlmForContent(concept, provider, model, apiKey, false);
    }

    private String generateSimplifiedConceptContent(Concept concept, String provider, String model, String apiKey) {
        return callLlmForContent(concept, provider, model, apiKey, true);
    }

    private String callLlmForContent(Concept concept, String provider, String model, String apiKey, boolean simplified) {
        try {
            var chatModel = chatModelFactory.createModel(provider, model, apiKey);
            String promptText;
            if (simplified) {
                promptText = "Explain the concept '" + concept.getTitle() + "' in VERY SIMPLE terms. "
                        + "Assume the reader is a complete beginner with no background knowledge. "
                        + "Include: 1) What is it? (one sentence), 2) Simple analogy, 3) A basic example. "
                        + "Use plain language, short sentences, and avoid jargon. "
                        + "Format with markdown for readability.";
            } else {
                promptText = "Explain the concept '" + concept.getTitle() + "' in a clear, educational way. "
                        + "Include: 1) Definition, 2) Key points, 3) Simple example. "
                        + "Keep it concise but thorough (2-3 paragraphs). "
                        + "Format with markdown for readability.";
            }
            List<Message> messages = List.of(
                    new SystemMessage("You are an expert educator. Explain concepts clearly."),
                    new UserMessage(promptText)
            );
            Prompt p = new Prompt(messages);
            return chatModel.call(p).getResult().getOutput().getText();
        } catch (Exception e) {
            return "## " + concept.getTitle() + "\n\nContent generation failed. Please try again later.";
        }
    }

    public Concept regenerateConceptContent(Long conceptId, boolean simplified, String provider, String model, String apiKey) {
        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> new RuntimeException("Concept not found"));
        String content = simplified
                ? generateSimplifiedConceptContent(concept, provider, model, apiKey)
                : generateConceptContent(concept, provider, model, apiKey);
        concept.setContent(content);
        return conceptRepository.save(concept);
    }

    private static class ChapterData {
        String title;
        Map<Integer, SectionData> sections;
        ChapterData(String title, Map<Integer, SectionData> sections) {
            this.title = title;
            this.sections = sections;
        }
    }

    private static class SectionData {
        String title;
        List<String> concepts;
        SectionData(String title, List<String> concepts) {
            this.title = title;
            this.concepts = concepts;
        }
    }
}
