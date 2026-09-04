package dev.rainbow.aidevelop.chat;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatStatusController {
    private final ChatProperties properties;

    ChatStatusController(ChatProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/status")
    ModelStatus status() {
        return new ModelStatus(properties.isEnabled(), properties.getProvider(), properties.getModel(),
                properties.getStreamMode());
    }

    public record ModelStatus(boolean enabled, String provider, String model, String streamMode) {
    }
}
