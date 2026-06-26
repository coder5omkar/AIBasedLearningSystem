package com.aitutor.config;

import com.aitutor.model.Subject;
import com.aitutor.repository.SubjectRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SubjectRepository subjectRepository;

    public DataInitializer(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @Override
    public void run(String... args) {
        if (subjectRepository.count() > 0) return;

        Subject[] subjects = {
            Subject.builder().name("Mathematics").category("Academic").description("Algebra, Geometry, Calculus, Statistics").icon("📐").displayOrder(1).build(),
            Subject.builder().name("Physics").category("Academic").description("Mechanics, Thermodynamics, Optics, Quantum").icon("⚛️").displayOrder(2).build(),
            Subject.builder().name("Chemistry").category("Academic").description("Organic, Inorganic, Physical, Biochemistry").icon("🧪").displayOrder(3).build(),
            Subject.builder().name("Biology").category("Academic").description("Cell Biology, Genetics, Ecology, Evolution").icon("🧬").displayOrder(4).build(),
            Subject.builder().name("English Literature").category("Academic").description("Poetry, Prose, Drama, Grammar").icon("📖").displayOrder(5).build(),
            Subject.builder().name("History").category("Academic").description("World History, Ancient Civilizations, Modern Era").icon("🏛️").displayOrder(6).build(),
            Subject.builder().name("Geography").category("Academic").description("Physical Geography, Human Geography, Cartography").icon("🌍").displayOrder(7).build(),
            Subject.builder().name("Computer Science").category("Technology").description("Data Structures, Algorithms, Systems, AI").icon("💻").displayOrder(8).build(),
            Subject.builder().name("Machine Learning").category("Technology").description("Supervised, Unsupervised, Deep Learning, NLP").icon("🤖").displayOrder(9).build(),
            Subject.builder().name("Web Development").category("Technology").description("HTML/CSS, JavaScript, React, Node.js, Databases").icon("🌐").displayOrder(10).build(),
            Subject.builder().name("Data Science").category("Technology").description("Python, Pandas, Visualization, Statistics, SQL").icon("📊").displayOrder(11).build(),
            Subject.builder().name("Cybersecurity").category("Technology").description("Network Security, Cryptography, Ethical Hacking").icon("🔒").displayOrder(12).build(),
            Subject.builder().name("Artificial Intelligence").category("Technology").description("Search Algorithms, Logic, Planning, Neural Networks").icon("🧠").displayOrder(13).build(),
            Subject.builder().name("Mobile Development").category("Technology").description("Android, iOS, React Native, Flutter").icon("📱").displayOrder(14).build(),
            Subject.builder().name("Cloud Computing").category("Technology").description("AWS, Azure, GCP, Docker, Kubernetes").icon("☁️").displayOrder(15).build(),
        };

        subjectRepository.saveAll(java.util.Arrays.asList(subjects));
    }
}
