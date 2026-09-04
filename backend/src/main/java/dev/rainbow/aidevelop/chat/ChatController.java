package dev.rainbow.aidevelop.chat;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService service;
    private final ChatProperties properties;

    ChatController(ChatService service, ChatProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(@Valid @RequestBody ChatStreamRequest request) {
        UUID requestId = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter(properties.getSseTimeout().toMillis());
        AtomicBoolean closed = new AtomicBoolean(false);
        AtomicReference<Disposable> subscription = new AtomicReference<>();

        emitter.onCompletion(() -> close(closed, subscription));
        emitter.onTimeout(() -> close(closed, subscription));
        emitter.onError(error -> close(closed, subscription));

        ChatRun run;
        try {
            run = service.start(requestId, request);
        } catch (RuntimeException error) {
            fail(emitter, closed, subscription, requestId, null, error);
            return emitter;
        }

        if (!send(emitter, closed, subscription, "metadata", ChatStreamEvent.metadata(requestId, run))
                || !send(emitter, closed, subscription, "phase",
                ChatStreamEvent.phase(requestId, run.conversationId(), "respond"))) {
            return emitter;
        }

        Disposable disposable = run.tokens()
                .subscribe(
                        token -> send(emitter, closed, subscription, "token",
                                ChatStreamEvent.token(requestId, run.conversationId(), token)),
                        error -> fail(emitter, closed, subscription, requestId, run.conversationId(), error),
                        () -> complete(emitter, closed, subscription, requestId, run.conversationId())
                );
        subscription.set(disposable);
        if (closed.get()) {
            disposable.dispose();
        }
        return emitter;
    }

    private boolean send(
            SseEmitter emitter,
            AtomicBoolean closed,
            AtomicReference<Disposable> subscription,
            String eventName,
            ChatStreamEvent event
    ) {
        if (closed.get()) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event().name(eventName).data(event));
            return true;
        } catch (IOException exception) {
            if (closed.compareAndSet(false, true)) {
                dispose(subscription);
                emitter.completeWithError(exception);
            }
            return false;
        }
    }

    private void complete(
            SseEmitter emitter,
            AtomicBoolean closed,
            AtomicReference<Disposable> subscription,
            UUID requestId,
            UUID conversationId
    ) {
        if (closed.get()) {
            return;
        }
        if (send(emitter, closed, subscription, "done", ChatStreamEvent.completed(requestId, conversationId))
                && closed.compareAndSet(false, true)) {
            emitter.complete();
        }
    }

    private void fail(
            SseEmitter emitter,
            AtomicBoolean closed,
            AtomicReference<Disposable> subscription,
            UUID requestId,
            UUID conversationId,
            Throwable error
    ) {
        String code = error instanceof ChatUnavailableException ? "AI_NOT_CONFIGURED" : "AI_STREAM_FAILED";
        String message = error instanceof ChatUnavailableException
                ? "AI model provider is not configured"
                : "AI response failed";
        if (send(emitter, closed, subscription, "error",
                ChatStreamEvent.failed(requestId, conversationId, code, message))
                && closed.compareAndSet(false, true)) {
            emitter.complete();
        }
    }

    private void close(AtomicBoolean closed, AtomicReference<Disposable> subscription) {
        if (closed.compareAndSet(false, true)) {
            dispose(subscription);
        }
    }

    private void dispose(AtomicReference<Disposable> subscription) {
        Disposable disposable = subscription.get();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
