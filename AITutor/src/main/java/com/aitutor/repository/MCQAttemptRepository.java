package com.aitutor.repository;

import com.aitutor.model.MCQAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MCQAttemptRepository extends JpaRepository<MCQAttempt, Long> {
    List<MCQAttempt> findByUserIdOrderByAttemptedAtDesc(Long userId);
    List<MCQAttempt> findByUserIdAndConceptIdOrderByAttemptedAtDesc(Long userId, Long conceptId);
    List<MCQAttempt> findByUserIdAndSubjectIdOrderByAttemptedAtDesc(Long userId, Long subjectId);
    List<MCQAttempt> findByUserIdAndConceptIdAndAttemptNumberOrderByQuestionNumberAsc(Long userId, Long conceptId, Integer attemptNumber);

    @Query("SELECT MAX(m.attemptNumber) FROM MCQAttempt m WHERE m.userId = :userId AND m.conceptId = :conceptId")
    Integer findMaxAttemptNumberByUserIdAndConceptId(@Param("userId") Long userId, @Param("conceptId") Long conceptId);
}
