package dev.rainbow.aidevelop.tool;

import dev.rainbow.aidevelop.skill.SkillRegistry;
import dev.rainbow.aidevelop.workspace.WorkspaceReader;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Map;

public class ReadOnlyProjectTools {
    private final WorkspaceReader workspaceReader;
    private final SkillRegistry skills;

    ReadOnlyProjectTools(WorkspaceReader workspaceReader, SkillRegistry skills) {
        this.workspaceReader = workspaceReader;
        this.skills = skills;
    }

    @Tool(description = "List the available Rainbow AI Dev Copilot skills and their versions")
    public Object listSkills() {
        return skills.list();
    }

    @Tool(description = "Read a bounded, text-only snapshot of a project below the configured workspace root")
    public Object readProjectSnapshot(
            @ToolParam(description = "Relative path below the configured workspace root") String relativePath) {
        var snapshot = workspaceReader.readProject(relativePath);
        return Map.of(
                "sources", snapshot.sources(),
                "truncated", snapshot.truncated(),
                "content", snapshot.content());
    }
}
