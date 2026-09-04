package dev.rainbow.aidevelop.run;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/runs")
public class AgentRunController {
    private final AgentRunService service;

    AgentRunController(AgentRunService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    AgentRunService.RunView get(@PathVariable UUID id) {
        return service.get(id);
    }
}
