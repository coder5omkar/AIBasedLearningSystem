package com.aitutor.repository;

import com.aitutor.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByChapterIdOrderBySectionOrderAsc(Long chapterId);
    void deleteByChapterId(Long chapterId);
}
