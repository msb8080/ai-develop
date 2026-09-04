package dev.rainbow.aidevelop.sandbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sandbox_jobs")
class SandboxJobEntity {
    @Id
    private UUID id;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(nullable = false)
    private String action;
    @Column(nullable = false)
    private String status;
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;
    @Column(name = "approved_at")
    private Instant approvedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "exit_code")
    private Integer exitCode;
    @Column(columnDefinition = "TEXT")
    private String output;
    @Column(columnDefinition = "TEXT")
    private String error;

    protected SandboxJobEntity() {}

    SandboxJobEntity(UUID id, UUID projectId, String action, Instant requestedAt) {
        this.id = id;
        this.projectId = projectId;
        this.action = action;
        this.status = "PENDING_APPROVAL";
        this.requestedAt = requestedAt;
    }

    void approve(Instant now) { status = "RUNNING"; approvedAt = now; }
    void reject(Instant now) { status = "REJECTED"; completedAt = now; }
    void complete(int code, String text, Instant now) {
        exitCode = code;
        output = text;
        completedAt = now;
        status = code == 0 ? "SUCCEEDED" : "FAILED";
        if (code != 0) error = "Command exited with code " + code;
    }
    void fail(String message, Instant now) { status = "FAILED"; error = message; completedAt = now; }
    void timeout(String text, Instant now) { status = "TIMED_OUT"; output = text; error = "Sandbox timed out"; completedAt = now; }

    UUID getId() { return id; }
    UUID getProjectId() { return projectId; }
    String getAction() { return action; }
    String getStatus() { return status; }
    Instant getRequestedAt() { return requestedAt; }
    Instant getApprovedAt() { return approvedAt; }
    Instant getCompletedAt() { return completedAt; }
    Integer getExitCode() { return exitCode; }
    String getOutput() { return output; }
    String getError() { return error; }
}
