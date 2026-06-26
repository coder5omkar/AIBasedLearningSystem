package com.aitutor.controller;

import com.aitutor.model.Subject;
import com.aitutor.service.SubjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping
    public ResponseEntity<List<Subject>> getAllSubjects() {
        return ResponseEntity.ok(subjectService.getAllSubjects());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Subject>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(subjectService.getSubjectsByCategory(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Subject> getSubject(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.getSubject(id));
    }
}
