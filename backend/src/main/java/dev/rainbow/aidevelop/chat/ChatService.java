package dev.rainbow.aidevelop.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rainbow.aidevelop.context.AssembledContext;
import dev.rainbow.aidevelop.context.ContextAssembler;
import dev.rainbow.aidevelop.conversation.ConversationMemoryService;
import dev.rainbow.aidevelop.skill.SkillRegistry;
import dev.rainbow.aidevelop.skill.SkillRegistry.SelectedSkill;
import dev.rainbow.aidevelop.run.AgentRunService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ObjectProvider<ChatGateway> gatewayProvider;
    private final ChatProperties properties;
    private final MeterRegistry meterRegistry;
    private final ConversationMemoryService memory;
    private final ContextAssembler contextAssembler;
    private final SkillRegistry skills;
    private final ObjectMapper objectMapper;
    private final AgentRunService runService;

    ChatService(
            ObjectProvider<ChatGateway> gatewayProvider,
            ChatProperties properties,
            MeterRegistry meterRegistry,
            ConversationMemoryService memory,
            ContextAssembler contextAssembler,
            SkillRegistry skills,
            ObjectMapper objectMapper,
            AgentRunService runService
    ) {
        this.gatewayProvider = gatewayProvider;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.memory = memory;
        this.contextAssembler = contextAssembler;
        this.skills = skills;
        this.objectMapper = objectMapper;
        this.runService = runService;
    }

    ChatRun start(UUID requestId, ChatStreamRequest request) {
        ChatGateway gateway = gatewayProvider.getIfAvailable();
        if (gateway == null) {
            meterRegistry.counter("rainbow.chat.requests", "result", "unavailable").increment();
            throw new ChatUnavailableException("AI model provider is not configured");
        }

        SelectedSkill skill = skills.select(request.skillId(), request.message()).orElse(null);
        var conversation = memory.prepare(request.conversationId(), request.projectId(), request.message(), requestId);
        AssembledContext context = contextAssembler.assemble(properties.getSystemPrompt(), request.message(),
                conversation.projectReference(), conversation.history(), skill);
        String skillId = skill == null ? null : skill.descriptor().id();
        UUID runId = runService.start(requestId, conversation.id(), context.sources(), context.truncated());
        StringBuilder answer = new StringBuilder();
        long startedAt = System.nanoTime();
        AtomicBoolean firstToken = new AtomicBoolean(true);
        meterRegistry.summary("rainbow.chat.input.characters").record(context.userPrompt().length());
        Flux<String> tokens = gateway.stream(context.systemPrompt(), context.userPrompt())
                .filter(token -> token != null && !token.isEmpty())
                .doOnNext(token -> {
                    answer.append(token);
                    if (firstToken.compareAndSet(true, false)) {
                        meterRegistry.timer("rainbow.chat.first_token.duration")
                                .record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
                    }
                })
                .doOnComplete(() -> {
                    memory.saveAssistant(conversation.id(), requestId, answer.toString(), metadata(skillId, context));
                    meterRegistry.summary("rainbow.chat.output.characters").record(answer.length());
                    runService.complete(runId, answer.length());
                    record(startedAt, "success", requestId);
                })
                .doOnError(error -> {
                    runService.fail(runId, error.getClass().getSimpleName());
                    record(startedAt, "failed", requestId);
                    log.warn("Chat stream failed: requestId={}, errorType={}",
                            requestId, error.getClass().getSimpleName());
                })
                .doOnCancel(() -> runService.cancel(runId));
        return new ChatRun(runId, conversation.id(), properties.getProvider(), properties.getModel(), skillId,
                context.sources(), context.truncated(), tokens);
    }

    private String metadata(String skillId, AssembledContext context) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "provider", properties.getProvider(),
                    "model", properties.getModel(),
                    "skill", skillId == null ? "none" : skillId,
                    "contextSourceCount", context.sources().size(),
                    "contextTruncated", context.truncated()));
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private void record(long startedAt, String result, UUID requestId) {
        meterRegistry.counter("rainbow.chat.requests", "result", result).increment();
        meterRegistry.timer("rainbow.chat.duration", "result", result)
                .record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
        if ("success".equals(result)) {
            log.info("Chat stream completed: requestId={}", requestId);
        }
    }
}
