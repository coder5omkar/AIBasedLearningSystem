package com.aitutor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_progress")
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long conceptId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Integer mcqAttempts;

    @Column(nullable = false)
    private Integer mcqCorrect;

    @Column(nullable = false)
    private Integer totalMcqQuestions;

    @Column(nullable = false)
    private Boolean mcqPassed;

    private LocalDateTime completedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
