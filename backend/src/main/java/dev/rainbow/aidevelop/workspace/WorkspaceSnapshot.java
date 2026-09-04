package dev.rainbow.aidevelop.workspace;

import java.util.List;

public record WorkspaceSnapshot(String content, List<String> sources, boolean truncated) {
    public static WorkspaceSnapshot empty() {
        return new WorkspaceSnapshot("", List.of(), false);
    }
}
