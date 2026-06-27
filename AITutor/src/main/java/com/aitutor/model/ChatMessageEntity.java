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
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String sessionId;

    @Column(nullable = false, length = 50)
    private String role;

    // Use @Lob for large text fields
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    private Long userId;

    private Long subjectId;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}