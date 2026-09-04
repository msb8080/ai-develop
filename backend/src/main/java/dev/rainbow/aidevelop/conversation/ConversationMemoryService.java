package dev.rainbow.aidevelop.conversation;

import dev.rainbow.aidevelop.project.ProjectEntity;
import dev.rainbow.aidevelop.project.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ConversationMemoryService {
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ProjectRepository projects;

    ConversationMemoryService(ConversationRepository conversations, MessageRepository messages,
                              ProjectRepository projects) {
        this.conversations = conversations;
        this.messages = messages;
        this.projects = projects;
    }

    @Transactional
    public PreparedConversation prepare(UUID conversationId, UUID projectId, String firstMessage, UUID requestId) {
        ConversationEntity conversation;
        if (conversationId == null) {
            ProjectEntity project = projectId == null ? null : projects.findById(projectId)
                    .orElseThrow(() -> new IllegalArgumentException("Project does not exist"));
            String normalized = firstMessage.strip().replaceAll("\\s+", " ");
            String title = normalized.length() > 60 ? normalized.substring(0, 60) + "…" : normalized;
            conversation = new ConversationEntity(UUID.randomUUID(), title, project, Instant.now());
            conversations.save(conversation);
        } else {
            conversation = conversations.findById(conversationId)
                    .orElseThrow(() -> new IllegalArgumentException("Conversation does not exist"));
            UUID existingProjectId = conversation.getProject() == null ? null : conversation.getProject().getId();
            if (projectId != null && !projectId.equals(existingProjectId)) {
                throw new IllegalArgumentException("Conversation belongs to a different project");
            }
        }

        List<MessageEntity> recent = messages.findTop12ByConversationIdOrderByCreatedAtDesc(conversation.getId());
        Collections.reverse(recent);
        messages.save(new MessageEntity(UUID.randomUUID(), conversation, "user", firstMessage.strip(),
                requestId, null, Instant.now()));
        conversation.touch(Instant.now());
        conversations.save(conversation);
        return new PreparedConversation(conversation.getId(),
                conversation.getProject() == null ? null : conversation.getProject().getSourceReference(),
                recent.stream().map(item -> new HistoryMessage(item.getRole(), item.getContent())).toList());
    }

    @Transactional
    public void saveAssistant(UUID conversationId, UUID requestId, String content, String metadataJson) {
        ConversationEntity conversation = conversations.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation does not exist"));
        messages.save(new MessageEntity(UUID.randomUUID(), conversation, "assistant", content,
                requestId, metadataJson, Instant.now()));
        conversation.touch(Instant.now());
        conversations.save(conversation);
    }

    public record PreparedConversation(UUID id, String projectReference, List<HistoryMessage> history) {
    }

    public record HistoryMessage(String role, String content) {
    }
}
