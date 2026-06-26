package com.aitutor.service;

import com.aitutor.model.Subject;
import com.aitutor.repository.SubjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public List<Subject> getAllSubjects() {
        return subjectRepository.findAllByOrderByDisplayOrderAsc();
    }

    public List<Subject> getSubjectsByCategory(String category) {
        return subjectRepository.findByCategoryOrderByDisplayOrderAsc(category);
    }

    public Subject getSubject(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
    }
}
