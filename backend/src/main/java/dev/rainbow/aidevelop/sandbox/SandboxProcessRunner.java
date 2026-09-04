package dev.rainbow.aidevelop.sandbox;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
class SandboxProcessRunner {
    SandboxExecution run(List<String> command, Duration timeout, int maxOutputCharacters) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readOutput(process, maxOutputCharacters));
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroy();
            if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly();
            removeContainer(command);
            return new SandboxExecution(-1, output.join(), true);
        }
        return new SandboxExecution(process.exitValue(), output.join(), false);
    }

    private void removeContainer(List<String> command) {
        int nameIndex = command.indexOf("--name");
        if (nameIndex < 0 || nameIndex + 1 >= command.size()) return;
        try {
            Process cleanup = new ProcessBuilder(command.get(0), "rm", "-f", command.get(nameIndex + 1))
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            cleanup.waitFor(5, TimeUnit.SECONDS);
        } catch (IOException exception) {
            // The persisted TIMED_OUT state remains visible; operators can remove the named container manually.
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private String readOutput(Process process, int maxCharacters) {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = process.inputReader(StandardCharsets.UTF_8)) {
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                if (result.length() < maxCharacters) {
                    int allowed = Math.min(read, maxCharacters - result.length());
                    result.append(buffer, 0, allowed);
                }
            }
        } catch (IOException exception) {
            if (result.length() < maxCharacters) result.append("\n[output unavailable]");
        }
        if (result.length() >= maxCharacters) result.append("\n[output truncated]");
        return result.toString();
    }
}
