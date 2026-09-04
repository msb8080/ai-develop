package dev.rainbow.aidevelop.skill;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class SkillRegistry {

    private final Map<String, SkillDescriptor> skills = new LinkedHashMap<>();

    public SkillRegistry() {
        register(new SkillDescriptor("code-review", "Java 代码审查",
                "基于文件证据发现正确性、安全性与测试缺口", "1.0.0",
                List.of("review", "审查", "代码问题", "安全问题")));
        register(new SkillDescriptor("spring-diagnosis", "Spring 故障诊断",
                "分析启动、配置、依赖、数据库与请求异常", "1.0.0",
                List.of("exception", "error", "启动失败", "报错", "故障", "spring")));
    }

    public List<SkillDescriptor> list() {
        return List.copyOf(skills.values());
    }

    public Optional<SelectedSkill> select(String requestedId, String message) {
        if (requestedId != null && !requestedId.isBlank()) {
            SkillDescriptor descriptor = Optional.ofNullable(skills.get(requestedId))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown skill: " + requestedId));
            return Optional.of(load(descriptor, "explicit"));
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return skills.values().stream()
                .filter(skill -> skill.triggers().stream().anyMatch(normalized::contains))
                .findFirst()
                .map(skill -> load(skill, "keyword"));
    }

    private SelectedSkill load(SkillDescriptor descriptor, String reason) {
        ClassPathResource resource = new ClassPathResource("skills/" + descriptor.id() + "/SKILL.md");
        try {
            String instructions = resource.getContentAsString(StandardCharsets.UTF_8);
            return new SelectedSkill(descriptor, instructions, reason);
        } catch (IOException exception) {
            throw new IllegalStateException("Skill instructions are unavailable: " + descriptor.id(), exception);
        }
    }

    private void register(SkillDescriptor descriptor) {
        skills.put(descriptor.id(), descriptor);
    }

    public record SelectedSkill(SkillDescriptor descriptor, String instructions, String reason) {
    }
}
