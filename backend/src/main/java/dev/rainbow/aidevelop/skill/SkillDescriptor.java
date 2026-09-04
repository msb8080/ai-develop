package dev.rainbow.aidevelop.skill;

import java.util.List;

public record SkillDescriptor(
        String id,
        String name,
        String description,
        String version,
        List<String> triggers
) {
}
