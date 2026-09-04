package dev.rainbow.aidevelop.chat;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatStreamEvent(
        UUID requestId,
        UUID runId,
        UUID conversationId,
        String phase,
        String provider,
        String model,
        String skillId,
        List<String> contextSources,
        Boolean contextTruncated,
        String content,
        String code,
        String message
) {
    static ChatStreamEvent metadata(UUID requestId, ChatRun run) {
        return new ChatStreamEvent(requestId, run.runId(), run.conversationId(), "context", run.provider(), run.model(),
                run.skillId(), run.contextSources(), run.contextTruncated(), null, null, null);
    }

    static ChatStreamEvent phase(UUID requestId, UUID conversationId, String phase) {
        return new ChatStreamEvent(requestId, null, conversationId, phase, null, null, null,
                null, null, null, null, null);
    }

    static ChatStreamEvent token(UUID requestId, UUID conversationId, String content) {
        return new ChatStreamEvent(requestId, null, conversationId, "respond", null, null, null,
                null, null, content, null, null);
    }

    static ChatStreamEvent completed(UUID requestId, UUID conversationId) {
        return new ChatStreamEvent(requestId, null, conversationId, "done", null, null, null,
                null, null, null, null, null);
    }

    static ChatStreamEvent failed(UUID requestId, UUID conversationId, String code, String message) {
        return new ChatStreamEvent(requestId, null, conversationId, "error", null, null, null,
                null, null, null, code, message);
    }
}
