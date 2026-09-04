package dev.rainbow.aidevelop.run;

import dev.rainbow.aidevelop.conversation.ConversationEntity;
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
@Table(name = "agent_runs")
public class AgentRunEntity {
    @Id
    private UUID id;
    @Column(name = "request_id")
    private UUID requestId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;
    private String status;
    @Column(name = "current_phase")
    private String currentPhase;
    private int iteration;
    @Column(name = "context_sources_json", columnDefinition = "text")
    private String contextSourcesJson;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;

    protected AgentRunEntity() {
    }

    AgentRunEntity(UUID id, UUID requestId, ConversationEntity conversation, String contextSourcesJson) {
        this.id = id;
        this.requestId = requestId;
        this.conversation = conversation;
        this.status = "RUNNING";
        this.currentPhase = "CONTEXT";
        this.iteration = 1;
        this.contextSourcesJson = contextSourcesJson;
        this.startedAt = Instant.now();
    }

    void transition(String status, String phase, boolean terminal) {
        this.status = status;
        this.currentPhase = phase;
        if (terminal) this.completedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRequestId() { return requestId; }
    public ConversationEntity getConversation() { return conversation; }
    public String getStatus() { return status; }
    public String getCurrentPhase() { return currentPhase; }
    public int getIteration() { return iteration; }
    public String getContextSourcesJson() { return contextSourcesJson; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
