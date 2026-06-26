package com.aitutor.repository;

import com.aitutor.model.Concept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConceptRepository extends JpaRepository<Concept, Long> {
    List<Concept> findBySectionIdOrderByConceptOrderAsc(Long sectionId);
    void deleteBySectionId(Long sectionId);
}
