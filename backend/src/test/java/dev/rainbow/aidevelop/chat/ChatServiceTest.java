package dev.rainbow.aidevelop.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rainbow.aidevelop.context.AssembledContext;
import dev.rainbow.aidevelop.context.ContextAssembler;
import dev.rainbow.aidevelop.conversation.ConversationMemoryService;
import dev.rainbow.aidevelop.skill.SkillRegistry;
import dev.rainbow.aidevelop.run.AgentRunService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class ChatServiceTest {

    @Test
    void delegatesToConfiguredGatewayAndFiltersEmptyTokens() {
        ChatGateway gateway = mock(ChatGateway.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ChatGateway> provider = mock(ObjectProvider.class);
        ChatProperties properties = new ChatProperties();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ConversationMemoryService memory = mock(ConversationMemoryService.class);
        ContextAssembler assembler = mock(ContextAssembler.class);
        SkillRegistry skills = mock(SkillRegistry.class);
        AgentRunService runService = mock(AgentRunService.class);
        UUID conversationId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        when(provider.getIfAvailable()).thenReturn(gateway);
        when(skills.select(null, "hello")).thenReturn(Optional.empty());
        when(memory.prepare(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                eq("hello"), any())).thenReturn(new ConversationMemoryService.PreparedConversation(
                conversationId, null, List.of()));
        when(assembler.assemble(eq(properties.getSystemPrompt()), eq("hello"),
                org.mockito.ArgumentMatchers.isNull(), eq(List.of()), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(new AssembledContext("system", "prompt", List.of(), false));
        when(gateway.stream("system", "prompt"))
                .thenReturn(Flux.just("你", "", "好"));
        when(runService.start(any(), eq(conversationId), eq(List.of()), eq(false))).thenReturn(runId);

        ChatService service = new ChatService(provider, properties, registry, memory, assembler, skills,
                new ObjectMapper(), runService);
        UUID requestId = UUID.randomUUID();
        ChatRun run = service.start(requestId, new ChatStreamRequest("hello", null, null, null));

        assertThat(run.tokens().collectList().block())
                .containsExactly("你", "好");
        verify(gateway).stream("system", "prompt");
        verify(memory).saveAssistant(eq(conversationId), eq(requestId), eq("你好"), any());
        verify(runService).complete(runId, 2);
        assertThat(registry.get("rainbow.chat.requests").tag("result", "success").counter().count())
                .isEqualTo(1);
    }

    @Test
    void failsClosedWhenNoGatewayIsConfigured() {
        @SuppressWarnings("unchecked")
        ObjectProvider<ChatGateway> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ChatService service = new ChatService(provider, new ChatProperties(), registry,
                mock(ConversationMemoryService.class), mock(ContextAssembler.class), mock(SkillRegistry.class),
                new ObjectMapper(), mock(AgentRunService.class));

        assertThatThrownBy(() -> service.start(UUID.randomUUID(),
                new ChatStreamRequest("hello", null, null, null)))
                .isInstanceOf(ChatUnavailableException.class)
                .hasMessage("AI model provider is not configured");
        assertThat(registry.get("rainbow.chat.requests").tag("result", "unavailable").counter().count())
                .isEqualTo(1);
    }
}
