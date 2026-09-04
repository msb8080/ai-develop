package dev.rainbow.aidevelop.run;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rainbow.aidevelop.conversation.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AgentRunService {
    private final AgentRunRepository runs;
    private final AgentEventRepository events;
    private final ConversationRepository conversations;
    private final ObjectMapper objectMapper;

    AgentRunService(AgentRunRepository runs, AgentEventRepository events,
                    ConversationRepository conversations, ObjectMapper objectMapper) {
        this.runs = runs;
        this.events = events;
        this.conversations = conversations;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID start(UUID requestId, UUID conversationId, List<String> sources, boolean truncated) {
        var conversation = conversations.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation does not exist"));
        UUID runId = UUID.randomUUID();
        AgentRunEntity run = runs.save(new AgentRunEntity(runId, requestId, conversation, json(sources)));
        events.save(new AgentEventEntity(run, 1, "CONTEXT", "CHECKPOINT",
                json(Map.of("sourceCount", sources.size(), "truncated", truncated))));
        run.transition("RUNNING", "RESPOND", false);
        runs.save(run);
        events.save(new AgentEventEntity(run, 2, "RESPOND", "MODEL_STARTED", "{}"));
        return runId;
    }

    @Transactional
    public void complete(UUID runId, int outputCharacters) {
        transition(runId, "COMPLETED", "DONE", "MODEL_COMPLETED",
                Map.of("outputCharacters", outputCharacters));
    }

    @Transactional
    public void fail(UUID runId, String errorType) {
        transition(runId, "FAILED", "ERROR", "MODEL_FAILED", Map.of("errorType", errorType));
    }

    @Transactional
    public void cancel(UUID runId) {
        transition(runId, "CANCELLED", "CANCELLED", "CLIENT_CANCELLED", Map.of());
    }

    @Transactional(readOnly = true)
    public RunView get(UUID id) {
        AgentRunEntity run = runs.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent run does not exist"));
        List<EventView> trace = events.findByRunIdOrderBySequenceNumber(id).stream()
                .map(event -> new EventView(event.getSequenceNumber(), event.getPhase(), event.getEventType(),
                        event.getPayloadJson(), event.getCreatedAt()))
                .toList();
        return new RunView(run.getId(), run.getRequestId(), run.getConversation().getId(), run.getStatus(),
                run.getCurrentPhase(), run.getIteration(), run.getContextSourcesJson(), run.getStartedAt(),
                run.getCompletedAt(), trace);
    }

    private void transition(UUID runId, String status, String phase, String eventType, Object payload) {
        AgentRunEntity run = runs.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Agent run does not exist"));
        run.transition(status, phase, true);
        runs.save(run);
        events.save(new AgentEventEntity(run, 3, phase, eventType, json(payload)));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    public record RunView(UUID id, UUID requestId, UUID conversationId, String status, String currentPhase,
                          int iteration, String contextSources, java.time.Instant startedAt,
                          java.time.Instant completedAt, List<EventView> events) {
    }

    public record EventView(int sequence, String phase, String eventType, String payload,
                            java.time.Instant createdAt) {
    }
}
