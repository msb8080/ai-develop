package dev.rainbow.aidevelop.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {
    List<ConversationEntity> findTop50ByOrderByUpdatedAtDesc();
}
