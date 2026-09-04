package dev.rainbow.aidevelop.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatStreamRequest(
        @NotBlank(message = "message must not be blank")
        @Size(max = 16_000, message = "message must not exceed 16000 characters")
        String message
) {
}
