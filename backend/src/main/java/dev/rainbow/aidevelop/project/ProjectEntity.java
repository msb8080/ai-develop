package dev.rainbow.aidevelop.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class ProjectEntity {

    @Id
    private UUID id;
    private String name;
    @Column(name = "source_type")
    private String sourceType;
    @Column(name = "source_reference")
    private String sourceReference;
    @Column(name = "created_at")
    private Instant createdAt;

    protected ProjectEntity() {
    }

    ProjectEntity(UUID id, String name, String sourceType, String sourceReference, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.sourceType = sourceType;
        this.sourceReference = sourceReference;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSourceType() { return sourceType; }
    public String getSourceReference() { return sourceReference; }
    public Instant getCreatedAt() { return createdAt; }
}
