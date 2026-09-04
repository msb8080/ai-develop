package dev.rainbow.aidevelop.chat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Flux;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    @Test
    void delegatesToConfiguredGatewayAndFiltersEmptyTokens() {
        ChatGateway gateway = mock(ChatGateway.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ChatGateway> provider = mock(ObjectProvider.class);
        ChatProperties properties = new ChatProperties();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(provider.getIfAvailable()).thenReturn(gateway);
        when(gateway.stream(properties.getSystemPrompt(), "hello"))
                .thenReturn(Flux.just("你", "", "好"));

        ChatService service = new ChatService(provider, properties, registry);

        assertThat(service.stream(UUID.randomUUID(), " hello ").collectList().block())
                .containsExactly("你", "好");
        verify(gateway).stream(properties.getSystemPrompt(), "hello");
        assertThat(registry.get("rainbow.chat.requests").tag("result", "success").counter().count())
                .isEqualTo(1);
    }

    @Test
    void failsClosedWhenNoGatewayIsConfigured() {
        @SuppressWarnings("unchecked")
        ObjectProvider<ChatGateway> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ChatService service = new ChatService(provider, new ChatProperties(), registry);

        assertThatThrownBy(() -> service.stream(UUID.randomUUID(), "hello").blockLast())
                .isInstanceOf(ChatUnavailableException.class)
                .hasMessage("AI model provider is not configured");
        assertThat(registry.get("rainbow.chat.requests").tag("result", "unavailable").counter().count())
                .isEqualTo(1);
    }
}
