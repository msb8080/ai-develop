package dev.rainbow.aidevelop.conversation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationRepository conversations;
    private final MessageRepository messages;

    ConversationController(ConversationRepository conversations, MessageRepository messages) {
        this.conversations = conversations;
        this.messages = messages;
    }

    @GetMapping
    List<ConversationView> list() {
        return conversations.findTop50ByOrderByUpdatedAtDesc().stream().map(ConversationView::from).toList();
    }

    @GetMapping("/{id}/messages")
    List<MessageView> messages(@PathVariable UUID id) {
        if (!conversations.existsById(id)) {
            throw new IllegalArgumentException("Conversation does not exist");
        }
        return messages.findByConversationIdOrderByCreatedAtAsc(id).stream().map(MessageView::from).toList();
    }

    public record ConversationView(UUID id, String title, UUID projectId, Instant createdAt, Instant updatedAt) {
        static ConversationView from(ConversationEntity entity) {
            return new ConversationView(entity.getId(), entity.getTitle(),
                    entity.getProject() == null ? null : entity.getProject().getId(),
                    entity.getCreatedAt(), entity.getUpdatedAt());
        }
    }

    public record MessageView(UUID id, String role, String content, UUID requestId, String metadata, Instant createdAt) {
        static MessageView from(MessageEntity entity) {
            return new MessageView(entity.getId(), entity.getRole(), entity.getContent(), entity.getRequestId(),
                    entity.getMetadataJson(), entity.getCreatedAt());
        }
    }
}
