package dev.rainbow.aidevelop.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class MessageEntity {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;
    private String role;
    @Column(columnDefinition = "text")
    private String content;
    @Column(name = "request_id")
    private UUID requestId;
    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;
    @Column(name = "created_at")
    private Instant createdAt;

    protected MessageEntity() {
    }

    public MessageEntity(UUID id, ConversationEntity conversation, String role, String content,
                         UUID requestId, String metadataJson, Instant createdAt) {
        this.id = id;
        this.conversation = conversation;
        this.role = role;
        this.content = content;
        this.requestId = requestId;
        this.metadataJson = metadataJson;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public ConversationEntity getConversation() { return conversation; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public UUID getRequestId() { return requestId; }
    public String getMetadataJson() { return metadataJson; }
    public Instant getCreatedAt() { return createdAt; }
}
