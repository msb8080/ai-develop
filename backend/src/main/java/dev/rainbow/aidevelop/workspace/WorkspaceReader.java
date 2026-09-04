package dev.rainbow.aidevelop.workspace;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Component
public class WorkspaceReader {

    private static final Set<String> EXCLUDED_PARTS = Set.of(".git", "target", "node_modules", "dist", ".idea");
    private static final Set<String> ALLOWED_NAMES = Set.of(
            "pom.xml", "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts",
            "application.yml", "application.yaml", "application.properties", "README.md", "AGENTS.md");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".java", ".kt", ".xml", ".yml", ".yaml", ".properties", ".md", ".json", ".ts", ".tsx");

    private final WorkspaceProperties properties;

    public WorkspaceReader(WorkspaceProperties properties) {
        this.properties = properties;
    }

    public String validateProjectReference(String reference) {
        Path raw = Path.of(reference.trim());
        if (raw.isAbsolute()) {
            throw new IllegalArgumentException("Project path must be relative to the configured workspace root");
        }
        Path root = workspaceRoot();
        Path candidate = root.resolve(raw).normalize();
        if (!candidate.startsWith(root) || !Files.isDirectory(candidate, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Project path is outside the workspace root or does not exist");
        }
        return root.relativize(candidate).toString().isBlank() ? "." : root.relativize(candidate).toString();
    }

    public WorkspaceSnapshot readProject(String reference) {
        Path root = workspaceRoot();
        Path project = root.resolve(validateProjectReference(reference)).normalize();
        List<Path> files;
        try (var stream = Files.walk(project, 12)) {
            files = stream.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.startsWith(project))
                    .filter(path -> isSafeSource(project, path))
                    .sorted(Comparator.comparing(path -> project.relativize(path).toString()))
                    .limit(properties.getMaxFiles() + 1L)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read project workspace", exception);
        }

        boolean truncated = files.size() > properties.getMaxFiles();
        StringBuilder content = new StringBuilder();
        List<String> sources = new ArrayList<>();
        for (Path file : files.stream().limit(properties.getMaxFiles()).toList()) {
            if (content.length() >= properties.getMaxTotalChars()) {
                truncated = true;
                break;
            }
            try {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                int allowed = Math.min(properties.getMaxFileChars(), properties.getMaxTotalChars() - content.length());
                String relative = project.relativize(file).toString();
                content.append("\n--- ").append(relative).append(" ---\n")
                        .append(text, 0, Math.min(text.length(), allowed));
                sources.add(relative);
                truncated |= text.length() > allowed;
            } catch (IOException | RuntimeException ignored) {
                // Unreadable or non-text files are deliberately skipped.
            }
        }
        return new WorkspaceSnapshot(content.toString(), List.copyOf(sources), truncated);
    }

    private Path workspaceRoot() {
        try {
            return Path.of(properties.getRoot()).toAbsolutePath().normalize().toRealPath();
        } catch (IOException exception) {
            throw new IllegalStateException("Configured workspace root is not readable", exception);
        }
    }

    private boolean isSafeSource(Path project, Path file) {
        Path relative = project.relativize(file);
        for (Path part : relative) {
            if (EXCLUDED_PARTS.contains(part.toString())) {
                return false;
            }
        }
        String name = file.getFileName().toString();
        if (name.startsWith(".env") || name.endsWith(".key") || name.endsWith(".pem")) {
            return false;
        }
        if (ALLOWED_NAMES.contains(name)) {
            return true;
        }
        return ALLOWED_EXTENSIONS.stream().anyMatch(name::endsWith);
    }
}
