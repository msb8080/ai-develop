package dev.rainbow.aidevelop.conversation;

import dev.rainbow.aidevelop.project.ProjectEntity;
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
@Table(name = "conversations")
public class ConversationEntity {
    @Id
    private UUID id;
    private String title;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    private ProjectEntity project;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected ConversationEntity() {
    }

    public ConversationEntity(UUID id, String title, ProjectEntity project, Instant now) {
        this.id = id;
        this.title = title;
        this.project = project;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void touch(Instant now) { this.updatedAt = now; }
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public ProjectEntity getProject() { return project; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
