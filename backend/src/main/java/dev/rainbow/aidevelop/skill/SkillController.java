package dev.rainbow.aidevelop.skill;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {
    private final SkillRegistry registry;

    SkillController(SkillRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    List<SkillDescriptor> list() {
        return registry.list();
    }
}
