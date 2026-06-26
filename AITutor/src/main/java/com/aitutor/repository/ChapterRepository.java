package com.aitutor.repository;

import com.aitutor.model.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findBySyllabusIdOrderByChapterOrderAsc(Long syllabusId);
    void deleteBySyllabusId(Long syllabusId);
}
