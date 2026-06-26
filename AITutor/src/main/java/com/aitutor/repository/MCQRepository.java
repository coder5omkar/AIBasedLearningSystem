package com.aitutor.repository;

import com.aitutor.model.MCQ;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MCQRepository extends JpaRepository<MCQ, Long> {
    List<MCQ> findByUserIdAndSessionIdOrderByQuestionNumberAsc(Long userId, String sessionId);

    List<MCQ> findByConceptId(Long conceptId);

    @Modifying
    @Transactional
    void deleteByUserIdAndSessionId(Long userId, String sessionId);
}
