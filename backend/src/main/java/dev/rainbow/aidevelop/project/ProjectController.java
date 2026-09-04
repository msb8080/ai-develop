package dev.rainbow.aidevelop.project;

import dev.rainbow.aidevelop.workspace.WorkspaceReader;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository repository;
    private final WorkspaceReader workspaceReader;

    ProjectController(ProjectRepository repository, WorkspaceReader workspaceReader) {
        this.repository = repository;
        this.workspaceReader = workspaceReader;
    }

    @GetMapping
    List<ProjectView> list() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(ProjectView::from)
                .toList();
    }

    @PostMapping
    ProjectView create(@Valid @RequestBody CreateProjectRequest request) {
        String safeReference = workspaceReader.validateProjectReference(request.relativePath());
        ProjectEntity entity = new ProjectEntity(
                UUID.randomUUID(), request.name().trim(), "LOCAL", safeReference, Instant.now());
        return ProjectView.from(repository.save(entity));
    }

    public record CreateProjectRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 500) String relativePath
    ) {
    }

    public record ProjectView(UUID id, String name, String sourceType, String sourceReference, Instant createdAt) {
        static ProjectView from(ProjectEntity entity) {
            return new ProjectView(entity.getId(), entity.getName(), entity.getSourceType(),
                    entity.getSourceReference(), entity.getCreatedAt());
        }
    }
}
