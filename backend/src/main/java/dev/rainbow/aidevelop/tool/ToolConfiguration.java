package dev.rainbow.aidevelop.tool;

import dev.rainbow.aidevelop.skill.SkillRegistry;
import dev.rainbow.aidevelop.workspace.WorkspaceReader;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfiguration {
    @Bean
    ToolCallbackProvider readOnlyProjectToolProvider(WorkspaceReader workspaceReader, SkillRegistry skills) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(new ReadOnlyProjectTools(workspaceReader, skills))
                .build();
    }
}
