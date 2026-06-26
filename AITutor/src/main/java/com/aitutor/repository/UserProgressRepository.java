package com.aitutor.repository;

import com.aitutor.model.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {
    List<UserProgress> findByUserId(Long userId);
    Optional<UserProgress> findByUserIdAndConceptId(Long userId, Long conceptId);
    Long countByUserIdAndStatus(Long userId, String status);
}
