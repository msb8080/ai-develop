package dev.rainbow.aidevelop.sandbox;

import dev.rainbow.aidevelop.project.ProjectEntity;
import dev.rainbow.aidevelop.project.ProjectRepository;
import dev.rainbow.aidevelop.workspace.WorkspaceReader;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

@Service
class SandboxService {
    private final SandboxProperties properties;
    private final SandboxJobRepository jobs;
    private final ProjectRepository projects;
    private final WorkspaceReader workspaceReader;
    private final SandboxProcessRunner runner;
    private final Semaphore executionSlot = new Semaphore(1);

    SandboxService(SandboxProperties properties, SandboxJobRepository jobs, ProjectRepository projects,
                   WorkspaceReader workspaceReader, SandboxProcessRunner runner) {
        this.properties = properties;
        this.jobs = jobs;
        this.projects = projects;
        this.workspaceReader = workspaceReader;
        this.runner = runner;
    }

    SandboxController.SandboxStatus status() {
        return new SandboxController.SandboxStatus(properties.isEnabled(), "explicit-approval", "none",
                properties.getMemory(), properties.getCpus(), properties.getTimeout().toString());
    }

    List<SandboxController.ActionView> actions() {
        return Arrays.stream(SandboxAction.values())
                .map(action -> new SandboxController.ActionView(action.id(), action.displayName(), action.description()))
                .toList();
    }

    List<SandboxController.JobView> list() {
        return jobs.findTop20ByOrderByRequestedAtDesc().stream().map(SandboxController.JobView::from).toList();
    }

    SandboxController.JobView plan(UUID projectId, String actionId) {
        requireEnabled();
        ProjectEntity project = projects.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project does not exist"));
        SandboxAction.fromId(actionId);
        workspaceReader.resolveProjectPath(project.getSourceReference());
        return SandboxController.JobView.from(jobs.save(
                new SandboxJobEntity(UUID.randomUUID(), projectId, actionId, Instant.now())));
    }

    SandboxController.JobView reject(UUID id) {
        SandboxJobEntity job = pending(id);
        job.reject(Instant.now());
        return SandboxController.JobView.from(jobs.save(job));
    }

    SandboxController.JobView approve(UUID id) {
        requireEnabled();
        SandboxJobEntity job = pending(id);
        if (!executionSlot.tryAcquire()) throw new SandboxBusyException("Another sandbox job is running");
        try {
            job.approve(Instant.now());
            jobs.save(job);
            ProjectEntity project = projects.findById(job.getProjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Project does not exist"));
            Path projectPath = workspaceReader.resolveProjectPath(project.getSourceReference());
            SandboxAction action = SandboxAction.fromId(job.getAction());
            SandboxExecution execution = runner.run(dockerCommand(job.getId(), projectPath, action),
                    properties.getTimeout(), properties.getMaxOutputCharacters());
            if (execution.timedOut()) job.timeout(execution.output(), Instant.now());
            else job.complete(execution.exitCode(), execution.output(), Instant.now());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            job.fail("Sandbox execution was interrupted", Instant.now());
        } catch (Exception exception) {
            job.fail("Sandbox execution failed: " + exception.getClass().getSimpleName(), Instant.now());
        } finally {
            executionSlot.release();
        }
        return SandboxController.JobView.from(jobs.save(job));
    }

    List<String> dockerCommand(UUID jobId, Path projectPath, SandboxAction action) {
        Path mavenRepository = resolveMavenRepository();
        List<String> command = new ArrayList<>(List.of(
                properties.getDockerBinary(), "run", "--rm",
                "--name", "rainbow-sandbox-" + jobId,
                "--network", "none",
                "--read-only",
                "--user", "65532:65532",
                "--cap-drop", "ALL",
                "--security-opt", "no-new-privileges",
                "--memory", properties.getMemory(),
                "--cpus", properties.getCpus(),
                "--pids-limit", String.valueOf(properties.getPidsLimit()),
                "--env", "HOME=/tmp/home",
                "--env", "MAVEN_CONFIG=/tmp/home/.m2",
                "--env", "MAVEN_OPTS=-Djansi.force=false -Djava.io.tmpdir=/tmp",
                "--tmpfs", "/tmp:rw,exec,nosuid,nodev,size=256m,mode=1777",
                "--tmpfs", "/workspace:rw,exec,nosuid,nodev,size=512m,mode=1777",
                "--mount", "type=bind,src=" + projectPath + ",dst=/source,readonly",
                "--mount", "type=bind,src=" + mavenRepository + ",dst=/m2,readonly",
                "--entrypoint", "sh",
                properties.getImage(),
                "-lc",
                "mkdir -p /tmp/home /workspace/backend"
                        + " && cp -R /source/backend/. /workspace/backend/"
                        + " && rm -rf /workspace/backend/target"
                        + " && exec " + String.join(" ", action.command())
        ));
        return List.copyOf(command);
    }

    private Path resolveMavenRepository() {
        String configured = properties.getMavenRepository();
        List<Path> candidates = configured == null || configured.isBlank()
                ? List.of(
                        Path.of(System.getProperty("user.home"), "tools", "maven", "repository"),
                        Path.of(System.getProperty("user.home"), ".m2", "repository"))
                : List.of(Path.of(configured));
        return candidates.stream()
                .map(path -> path.toAbsolutePath().normalize())
                .filter(Files::isDirectory)
                .filter(path -> Files.isDirectory(path.resolve("org/springframework/boot/spring-boot-starter-parent")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("A populated local Maven repository is unavailable"));
    }

    private SandboxJobEntity pending(UUID id) {
        SandboxJobEntity job = jobs.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sandbox job does not exist"));
        if (!"PENDING_APPROVAL".equals(job.getStatus())) {
            throw new IllegalArgumentException("Sandbox job is no longer awaiting approval");
        }
        return job;
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) throw new SandboxBusyException("Sandbox is disabled");
    }
}
