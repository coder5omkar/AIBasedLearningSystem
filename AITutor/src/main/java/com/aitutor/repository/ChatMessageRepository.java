package com.aitutor.repository;

import com.aitutor.model.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findByUserIdAndSessionIdOrderByTimestampAsc(Long userId, String sessionId);

    List<ChatMessageEntity> findByUserIdAndSubjectIdOrderByTimestampAsc(Long userId, Long subjectId);

    @Query("SELECT DISTINCT c.sessionId FROM ChatMessageEntity c WHERE c.userId = :userId")
    List<String> findDistinctSessionIdsByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ChatMessageEntity c WHERE c.userId = :userId AND c.sessionId = :sessionId")
    void deleteByUserIdAndSessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);
}
