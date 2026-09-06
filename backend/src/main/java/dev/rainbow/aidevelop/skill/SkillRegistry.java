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
        register(new SkillDescriptor("general", "通用助手", "问答、翻译和写作", "1.0.0", List.of()));
        register(new SkillDescriptor("prd", "需求分析", "用户故事、范围与验收标准", "1.0.0", List.of("/prd")));
        register(new SkillDescriptor("design", "交互设计", "用户流程、状态和可访问性", "1.0.0", List.of("/design")));
        register(new SkillDescriptor("code", "编码与重构", "基于证据给出实现建议", "1.0.0", List.of()));
        register(new SkillDescriptor("test", "测试设计", "边界、集成与回归用例", "1.0.0", List.of()));
        register(new SkillDescriptor("deploy", "部署规划", "部署配置与回滚建议，不执行命令", "1.0.0", List.of()));
        register(new SkillDescriptor("chef", "烹饪助手", "根据食材和偏好组织菜谱", "1.0.0", List.of()));
        register(new SkillDescriptor("stock", "金融知识", "解释概念，不声称掌握实时行情", "1.0.0", List.of()));
        register(new SkillDescriptor("gamer", "游戏助手", "玩法与攻略，不编造最新版本", "1.0.0", List.of()));
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
        String command = normalized.strip().split("\\s+", 2)[0];
        String commandSkill = Map.of("/explain", "code", "/optimize", "code", "/bug", "spring-diagnosis",
                "/test", "test", "/refactor", "code", "/doc", "code", "/review", "code-review").get(command);
        if (commandSkill != null) return Optional.of(load(skills.get(commandSkill), "command"));
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
