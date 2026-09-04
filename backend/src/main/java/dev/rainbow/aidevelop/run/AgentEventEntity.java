package dev.rainbow.aidevelop.run;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "agent_events")
public class AgentEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private AgentRunEntity run;
    @Column(name = "sequence_number")
    private int sequenceNumber;
    private String phase;
    @Column(name = "event_type")
    private String eventType;
    @Column(name = "payload_json", columnDefinition = "text")
    private String payloadJson;
    @Column(name = "created_at")
    private Instant createdAt;

    protected AgentEventEntity() {
    }

    AgentEventEntity(AgentRunEntity run, int sequenceNumber, String phase, String eventType, String payloadJson) {
        this.run = run;
        this.sequenceNumber = sequenceNumber;
        this.phase = phase;
        this.eventType = eventType;
        this.payloadJson = payloadJson;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public AgentRunEntity getRun() { return run; }
    public int getSequenceNumber() { return sequenceNumber; }
    public String getPhase() { return phase; }
    public String getEventType() { return eventType; }
    public String getPayloadJson() { return payloadJson; }
    public Instant getCreatedAt() { return createdAt; }
}
