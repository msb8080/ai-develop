package dev.rainbow.aidevelop.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "rainbow.ai.chat", name = "enabled", havingValue = "true")
class SpringAiChatGateway implements ChatGateway {

    private final ChatClient chatClient;
    private final ChatProperties properties;

    SpringAiChatGateway(ChatClient.Builder builder, ChatProperties properties) {
        this.chatClient = builder.build();
        this.properties = properties;
    }

    @Override
    public Flux<String> stream(String systemPrompt, String message) {
        if ("buffered".equalsIgnoreCase(properties.getStreamMode())) {
            return Mono.fromCallable(() -> chatClient.prompt()
                            .system(systemPrompt)
                            .user(message)
                            .call()
                            .content())
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMapMany(this::chunks);
        }
        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .stream()
                .content();
    }

    private Flux<String> chunks(String content) {
        if (content == null || content.isBlank()) {
            return Flux.error(new IllegalStateException("Model returned an empty response"));
        }
        int[] points = content.codePoints().toArray();
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < points.length; start += 12) {
            chunks.add(new String(points, start, Math.min(12, points.length - start)));
        }
        return Flux.fromIterable(chunks);
    }
}
