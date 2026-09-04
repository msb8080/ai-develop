package dev.rainbow.aidevelop.sandbox;

import dev.rainbow.aidevelop.project.ProjectRepository;
import dev.rainbow.aidevelop.workspace.WorkspaceReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SandboxServiceTest {
    @TempDir
    Path temporaryRepository;

    @Test
    void dockerCommandAppliesIsolationAndUsesOnlyWhitelistedAction() throws IOException {
        Files.createDirectories(temporaryRepository.resolve("org/springframework/boot/spring-boot-starter-parent"));
        SandboxProperties properties = new SandboxProperties();
        properties.setEnabled(true);
        properties.setMavenRepository(temporaryRepository.toString());
        SandboxService service = new SandboxService(
                properties,
                mock(SandboxJobRepository.class),
                mock(ProjectRepository.class),
                mock(WorkspaceReader.class),
                mock(SandboxProcessRunner.class));

        List<String> command = service.dockerCommand(UUID.randomUUID(), Path.of("/tmp/safe-project"),
                SandboxAction.BACKEND_TEST);

        assertThat(command).containsSubsequence("--network", "none");
        assertThat(command).contains("--read-only", "--cap-drop", "ALL", "no-new-privileges");
        assertThat(command).containsSubsequence("--user", "65532:65532");
        assertThat(command).containsSubsequence("--mount", "type=bind,src=/tmp/safe-project,dst=/source,readonly");
        assertThat(command).contains("/workspace:rw,exec,nosuid,nodev,size=512m,mode=1777");
        assertThat(command.get(command.size() - 1)).endsWith("-f /workspace/backend/pom.xml test");
    }
}
