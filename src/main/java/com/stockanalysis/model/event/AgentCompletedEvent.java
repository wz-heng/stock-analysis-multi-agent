package com.stockanalysis.model.event;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AgentCompletedEvent extends ApplicationEvent {
    private final String sessionId;
    private final AnalysisStage stage;
    private final String message;
    private final Object data;

    public AgentCompletedEvent(Object source, String sessionId,
                                AnalysisStage stage, String message, Object data) {
        super(source);
        this.sessionId = sessionId;
        this.stage = stage;
        this.message = message;
        this.data = data;
    }
}
