package dev.rainbow.aidevelop.sandbox;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sandbox")
class SandboxController {
    private final SandboxService service;

    SandboxController(SandboxService service) { this.service = service; }

    @GetMapping("/status") SandboxStatus status() { return service.status(); }
    @GetMapping("/actions") List<ActionView> actions() { return service.actions(); }
    @GetMapping("/jobs") List<JobView> jobs() { return service.list(); }

    @PostMapping("/jobs")
    JobView plan(@Valid @RequestBody PlanRequest request) {
        return service.plan(request.projectId(), request.action());
    }

    @PostMapping("/jobs/{id}/approve") JobView approve(@PathVariable UUID id) { return service.approve(id); }
    @PostMapping("/jobs/{id}/reject") JobView reject(@PathVariable UUID id) { return service.reject(id); }

    record PlanRequest(@NotNull UUID projectId, @NotBlank String action) {}
    record ActionView(String id, String name, String description) {}
    record SandboxStatus(boolean enabled, String approval, String network, String memory, String cpus, String timeout) {}
    record JobView(UUID id, UUID projectId, String action, String status, Instant requestedAt, Instant approvedAt,
                   Instant completedAt, Integer exitCode, String output, String error) {
        static JobView from(SandboxJobEntity entity) {
            return new JobView(entity.getId(), entity.getProjectId(), entity.getAction(), entity.getStatus(),
                    entity.getRequestedAt(), entity.getApprovedAt(), entity.getCompletedAt(), entity.getExitCode(),
                    entity.getOutput(), entity.getError());
        }
    }
}
