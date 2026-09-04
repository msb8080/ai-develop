package dev.rainbow.aidevelop.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChatStreamRequest(
        @NotBlank(message = "message must not be blank")
        @Size(max = 16_000, message = "message must not exceed 16000 characters")
        String message,
        UUID conversationId,
        UUID projectId,
        @Size(max = 80, message = "skillId must not exceed 80 characters") String skillId
) {
}
