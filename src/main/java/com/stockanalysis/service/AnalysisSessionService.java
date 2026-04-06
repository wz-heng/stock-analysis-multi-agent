package com.stockanalysis.service;

import com.stockanalysis.model.event.AgentCompletedEvent;
import com.stockanalysis.model.event.AnalysisStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnalysisSessionService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisSessionService.class);

    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, Sinks.Many<String>> sinks = new ConcurrentHashMap<>();

    public AnalysisSessionService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        sinks.put(sessionId, sink);
        log.info("Created analysis session: {}", sessionId);
        return sessionId;
    }

    public Flux<String> getStream(String sessionId) {
        Sinks.Many<String> sink = sinks.get(sessionId);
        if (sink == null) {
            return Flux.error(new IllegalArgumentException("Session not found: " + sessionId));
        }
        return sink.asFlux();
    }

    public void publishEvent(String sessionId, AnalysisStage stage, String message, Object data) {
        log.debug("Publishing event for session {}: stage={}, message={}", sessionId, stage, message);
        eventPublisher.publishEvent(new AgentCompletedEvent(this, sessionId, stage, message, data));

        Sinks.Many<String> sink = sinks.get(sessionId);
        if (sink != null) {
            String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
            String ssePayload = String.format(
                "{\"stage\":\"%s\",\"message\":\"%s\"}", stage.name(), escaped);
            sink.tryEmitNext(ssePayload);

            if (stage == AnalysisStage.ANALYSIS_COMPLETED || stage == AnalysisStage.ANALYSIS_FAILED) {
                sink.tryEmitComplete();
                sinks.remove(sessionId);
                log.info("Session {} completed, sink removed", sessionId);
            }
        }
    }
}
