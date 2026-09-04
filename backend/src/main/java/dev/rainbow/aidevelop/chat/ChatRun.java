package dev.rainbow.aidevelop.chat;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

public record ChatRun(
        UUID runId,
        UUID conversationId,
        String provider,
        String model,
        String skillId,
        List<String> contextSources,
        boolean contextTruncated,
        Flux<String> tokens
) {
}
