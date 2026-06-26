package com.aitutor.repository;

import com.aitutor.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findAllByOrderByDisplayOrderAsc();
    List<Subject> findByCategoryOrderByDisplayOrderAsc(String category);
}
