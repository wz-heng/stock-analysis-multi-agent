package com.stockanalysis.web;

import com.stockanalysis.data.TushareProvider;
import com.stockanalysis.model.stock.StockInfo;
import com.stockanalysis.service.AnalysisSessionService;
import com.stockanalysis.service.DebateService;
import com.stockanalysis.service.PipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AnalysisController {

    private final AnalysisSessionService sessionService;
    private final PipelineService pipelineService;
    private final DebateService debateService;
    private final TushareProvider tushareProvider;

    /**
     * 启动分析
     * mode: "pipeline" | "debate" | "combined"
     * dateRange: "1m" | "3m" | "6m" | "1y"
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startAnalysis(@RequestBody StartRequest request) {
        log.info("Starting analysis: code={}, mode={}, range={}",
                 request.stockCode(), request.mode(), request.dateRange());
        String sessionId = sessionService.createSession();

        switch (request.mode()) {
            case "pipeline" -> pipelineService.runPipeline(
                    sessionId, request.stockCode(), request.dateRange());
            case "debate" -> {
                StockInfo info = tushareProvider.getStockInfo(request.stockCode());
                String context = debateService.buildDebateContext(
                        info.getCode(), info.getName(), info.getIndustry());
                debateService.runDebate(sessionId, context);
            }
            case "combined" -> {
                // 先跑流水线，流水线完成后触发辩论（由 PipelineService 内部事件驱动，此处仅启动流水线）
                pipelineService.runPipeline(sessionId, request.stockCode(), request.dateRange());
            }
            default -> {
                log.warn("Unknown mode: {}, defaulting to pipeline", request.mode());
                pipelineService.runPipeline(sessionId, request.stockCode(), request.dateRange());
            }
        }

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "status", "started",
                "stockCode", request.stockCode(),
                "mode", request.mode()
        ));
    }

    public record StartRequest(String stockCode, String mode, String dateRange) {}
}
