package dev.rainbow.aidevelop.chat;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ObjectProvider<ChatGateway> gatewayProvider;
    private final ChatProperties properties;
    private final MeterRegistry meterRegistry;

    ChatService(
            ObjectProvider<ChatGateway> gatewayProvider,
            ChatProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.gatewayProvider = gatewayProvider;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    Flux<String> stream(UUID requestId, String message) {
        ChatGateway gateway = gatewayProvider.getIfAvailable();
        if (gateway == null) {
            meterRegistry.counter("rainbow.chat.requests", "result", "unavailable").increment();
            return Flux.error(new ChatUnavailableException("AI model provider is not configured"));
        }

        long startedAt = System.nanoTime();
        return gateway.stream(properties.getSystemPrompt(), message.trim())
                .filter(token -> token != null && !token.isEmpty())
                .doOnComplete(() -> {
                    meterRegistry.counter("rainbow.chat.requests", "result", "success").increment();
                    meterRegistry.timer("rainbow.chat.duration", "result", "success")
                            .record(System.nanoTime() - startedAt, java.util.concurrent.TimeUnit.NANOSECONDS);
                    log.info("Chat stream completed: requestId={}", requestId);
                })
                .doOnError(error -> {
                    meterRegistry.counter("rainbow.chat.requests", "result", "failed").increment();
                    meterRegistry.timer("rainbow.chat.duration", "result", "failed")
                            .record(System.nanoTime() - startedAt, java.util.concurrent.TimeUnit.NANOSECONDS);
                    log.warn("Chat stream failed: requestId={}, errorType={}",
                            requestId, error.getClass().getSimpleName());
                });
    }
}
