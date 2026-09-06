package dev.rainbow.aidevelop.skill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillRegistryTest {
    private final SkillRegistry registry = new SkillRegistry();
    @Test
    void migratedRolesAllLoadAndCommandsRouteBeforeKeywords() {
        assertThat(registry.list()).hasSize(11);
        registry.list().forEach(skill -> assertThat(registry.select(skill.id(), "请求").orElseThrow().instructions()).isNotBlank());
        assertThat(registry.select(null, "/test Spring 服务").orElseThrow().descriptor().id()).isEqualTo("test");
        assertThat(registry.select(null, "/refactor 服务").orElseThrow().reason()).isEqualTo("command");
        assertThat(registry.select(null, "/testing")).isEmpty();
    }

    @Test
    void loadsExplicitSkillInstructionsAndVersion() {
        var selected = registry.select("code-review", "普通请求").orElseThrow();

        assertThat(selected.descriptor().version()).isEqualTo("1.0.0");
        assertThat(selected.instructions()).contains("Java Code Review");
        assertThat(selected.reason()).isEqualTo("explicit");
    }

    @Test
    void routesSpringFailureByKeyword() {
        var selected = registry.select(null, "Spring 启动失败了").orElseThrow();

        assertThat(selected.descriptor().id()).isEqualTo("spring-diagnosis");
        assertThat(selected.reason()).isEqualTo("keyword");
    }
}
