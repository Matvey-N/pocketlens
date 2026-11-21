package com.pocketlens.assistant.persistence;

import com.pocketlens.assistant.domain.Message;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    @Query("select m from Message m where m.conversationId = :conversationId order by m.createdAt desc")
    List<Message> findLatest(UUID conversationId, Pageable pageable);

    Optional<Message> findByIdAndConversationId(UUID id, UUID conversationId);

    long countByRoleAndCreatedAtAfter(Message.Role role, Instant after);
}
