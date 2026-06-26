package com.aitutor.repository;

import com.aitutor.model.Syllabus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SyllabusRepository extends JpaRepository<Syllabus, Long> {
    List<Syllabus> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Syllabus> findByIdAndUserId(Long id, Long userId);
}
