package dev.rainbow.aidevelop.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkspaceReaderTest {
    @TempDir
    Path root;

    @Test
    void readsBoundedSourcesAndSkipsSecretsAndBuildOutputs() throws Exception {
        Files.createDirectories(root.resolve("demo/src"));
        Files.createDirectories(root.resolve("demo/target"));
        Files.writeString(root.resolve("demo/src/App.java"), "class App {}");
        Files.writeString(root.resolve("demo/.env"), "SECRET=never-read");
        Files.writeString(root.resolve("demo/target/Generated.java"), "class Generated {}");
        WorkspaceProperties properties = properties();
        WorkspaceReader reader = new WorkspaceReader(properties);

        WorkspaceSnapshot snapshot = reader.readProject("demo");

        assertThat(snapshot.sources()).containsExactly("src/App.java");
        assertThat(snapshot.content()).contains("class App").doesNotContain("never-read", "Generated");
    }

    @Test
    void rejectsAbsoluteAndEscapingPaths() {
        WorkspaceReader reader = new WorkspaceReader(properties());

        assertThatThrownBy(() -> reader.validateProjectReference(root.toString()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reader.validateProjectReference("../outside"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private WorkspaceProperties properties() {
        WorkspaceProperties properties = new WorkspaceProperties();
        properties.setRoot(root.toString());
        properties.setMaxFiles(5);
        properties.setMaxFileChars(1000);
        properties.setMaxTotalChars(2000);
        return properties;
    }
}
