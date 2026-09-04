package dev.rainbow.aidevelop.chat;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatStreamEvent(
        UUID requestId,
        String content,
        String code,
        String message
) {
    static ChatStreamEvent metadata(UUID requestId) {
        return new ChatStreamEvent(requestId, null, null, null);
    }

    static ChatStreamEvent token(UUID requestId, String content) {
        return new ChatStreamEvent(requestId, content, null, null);
    }

    static ChatStreamEvent completed(UUID requestId) {
        return new ChatStreamEvent(requestId, null, null, null);
    }

    static ChatStreamEvent failed(UUID requestId, String code, String message) {
        return new ChatStreamEvent(requestId, null, code, message);
    }
}
