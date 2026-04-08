package com.stockanalysis.web;

import com.stockanalysis.service.AnalysisSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class SseController {

    private final AnalysisSessionService sessionService;

    /**
     * SSE 实时推送分析进度
     * 客户端：const es = new EventSource('/api/analysis/{sessionId}/stream')
     */
    @GetMapping(value = "/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@PathVariable String sessionId) {
        return sessionService.getStream(sessionId);
    }
}
