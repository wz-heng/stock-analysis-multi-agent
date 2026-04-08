package com.stockanalysis.web;

import com.stockanalysis.service.AnalysisSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AnalysisController {

    private final AnalysisSessionService sessionService;

    /**
     * 启动分析，返回 sessionId 供前端建立 SSE 连接
     * mode: pipeline / debate / combined
     * dateRange: 1m / 3m / 6m / 1y
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startAnalysis(@RequestBody StartRequest request) {
        String sessionId = sessionService.createSession();
        // AI Agent 层（Plan B）将在此处注入 PipelineService/DebateService 调用
        return ResponseEntity.ok(Map.of(
            "sessionId", sessionId,
            "status", "started",
            "stockCode", request.stockCode(),
            "mode", request.mode()
        ));
    }

    public record StartRequest(String stockCode, String mode, String dateRange) {}
}
