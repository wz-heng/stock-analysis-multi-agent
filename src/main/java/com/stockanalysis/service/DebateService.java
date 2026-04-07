package com.stockanalysis.service;

import com.stockanalysis.agent.debate.*;
import com.stockanalysis.model.event.AnalysisStage;
import com.stockanalysis.model.report.ArbitratorDecision;
import com.stockanalysis.model.report.DebateArgument;
import com.stockanalysis.model.report.Stance;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class DebateService {

    private final ChatLanguageModel gpt4oModel;
    private final ChatLanguageModel anthropicModel;
    private final ChatLanguageModel deepSeekModel;
    private final AnalysisSessionService sessionService;
    private final LlmResponseParser parser;

    private BullAgentService bullAgent;
    private BearAgentService bearAgent;
    private NeutralAgentService neutralAgent;
    private ArbitratorAgentService arbitratorAgent;

    public DebateService(
            ChatLanguageModel gpt4oModel,
            @Qualifier("anthropicChatModel") ChatLanguageModel anthropicModel,
            @Qualifier("deepSeekChatModel") ChatLanguageModel deepSeekModel,
            AnalysisSessionService sessionService,
            LlmResponseParser parser) {
        this.gpt4oModel = gpt4oModel;
        this.anthropicModel = anthropicModel;
        this.deepSeekModel = deepSeekModel;
        this.sessionService = sessionService;
        this.parser = parser;
    }

    @PostConstruct
    void initAgents() {
        bullAgent = AiServices.builder(BullAgentService.class)
                .chatLanguageModel(gpt4oModel).build();
        bearAgent = AiServices.builder(BearAgentService.class)
                .chatLanguageModel(anthropicModel).build();
        neutralAgent = AiServices.builder(NeutralAgentService.class)
                .chatLanguageModel(deepSeekModel).build();
        arbitratorAgent = AiServices.builder(ArbitratorAgentService.class)
                .chatLanguageModel(gpt4oModel).build();
        log.info("Debate agents initialized");
    }

    @Async
    public void runDebate(String sessionId, String debateContext) {
        try {
            // 三方并行辩论
            sessionService.publishEvent(sessionId, AnalysisStage.BULL_STARTED, "多方开始立论...", null);
            sessionService.publishEvent(sessionId, AnalysisStage.BEAR_STARTED, "空方开始立论...", null);
            sessionService.publishEvent(sessionId, AnalysisStage.NEUTRAL_STARTED, "中立方开始评估...", null);

            CompletableFuture<String> bullFuture = CompletableFuture.supplyAsync(
                    () -> bullAgent.argue(debateContext));
            CompletableFuture<String> bearFuture = CompletableFuture.supplyAsync(
                    () -> bearAgent.argue(debateContext));
            CompletableFuture<String> neutralFuture = CompletableFuture.supplyAsync(
                    () -> neutralAgent.argue(debateContext));

            CompletableFuture.allOf(bullFuture, bearFuture, neutralFuture).join();

            String bullResponse    = bullFuture.get();
            String bearResponse    = bearFuture.get();
            String neutralResponse = neutralFuture.get();

            DebateArgument bullArg    = parseArgument(bullResponse, Stance.BULL, "GPT-4o");
            DebateArgument bearArg    = parseArgument(bearResponse, Stance.BEAR, "Claude");
            DebateArgument neutralArg = parseArgument(neutralResponse, Stance.NEUTRAL, "DeepSeek");

            sessionService.publishEvent(sessionId, AnalysisStage.BULL_COMPLETED,
                    "多方完成立论", bullArg);
            sessionService.publishEvent(sessionId, AnalysisStage.BEAR_COMPLETED,
                    "空方完成立论", bearArg);
            sessionService.publishEvent(sessionId, AnalysisStage.NEUTRAL_COMPLETED,
                    "中立方完成评估", neutralArg);

            // 仲裁官综合裁决
            sessionService.publishEvent(sessionId, AnalysisStage.ARBITRATOR_STARTED,
                    "仲裁官正在综合裁决...", null);

            String arbitratorContext  = buildArbitratorContext(bullArg, bearArg, neutralArg);
            String arbitratorResponse = arbitratorAgent.adjudicate(arbitratorContext);

            ArbitratorDecision decision = new ArbitratorDecision();
            decision.setFinalRating(parser.getString(arbitratorResponse, "finalRating", "持有"));
            decision.setConfidencePercent(parser.getInt(arbitratorResponse, "confidencePercent", 50));
            decision.setStrongestArgument(parser.getString(arbitratorResponse, "strongestArgument", "NEUTRAL"));
            decision.setWeakestArgument(parser.getString(arbitratorResponse, "weakestArgument", "NEUTRAL"));
            decision.setSynthesisText(parser.getString(arbitratorResponse, "synthesisText", arbitratorResponse));
            decision.setNeedsSecondRound(parser.getBoolean(arbitratorResponse, "needsSecondRound", false));
            decision.setSecondRoundFocus(parser.getString(arbitratorResponse, "secondRoundFocus", ""));
            decision.setDebateArguments(List.of(bullArg, bearArg, neutralArg));

            sessionService.publishEvent(sessionId, AnalysisStage.ARBITRATOR_COMPLETED,
                    "仲裁官裁决：" + decision.getFinalRating() + "（置信度 " + decision.getConfidencePercent() + "%）",
                    decision);
            sessionService.publishEvent(sessionId, AnalysisStage.ANALYSIS_COMPLETED,
                    "辩论分析完成", decision);

        } catch (Exception e) {
            log.error("Debate failed for session {}", sessionId, e);
            sessionService.publishEvent(sessionId, AnalysisStage.ANALYSIS_FAILED,
                    "辩论失败：" + e.getMessage(), null);
        }
    }

    private DebateArgument parseArgument(String response, Stance stance, String model) {
        DebateArgument arg = new DebateArgument();
        arg.setStance(stance);
        arg.setModel(model);
        arg.setMainPoints(parser.getString(response, "mainPoints", ""));
        arg.setEvidence(parser.getString(response, "evidence", ""));
        arg.setConclusion(parser.getString(response, "conclusion", response));
        return arg;
    }

    private String buildArbitratorContext(DebateArgument bull, DebateArgument bear, DebateArgument neutral) {
        return String.format("""
                以下是多方、空方、中立方的辩论论点，请进行综合裁决：

                【多方论点（GPT-4o）】
                核心观点：%s
                支撑证据：%s
                结论：%s

                【空方论点（Claude）】
                核心观点：%s
                支撑证据：%s
                结论：%s

                【中立评估（DeepSeek）】
                核心观点：%s
                关键数据：%s
                结论：%s
                """,
                bull.getMainPoints(), bull.getEvidence(), bull.getConclusion(),
                bear.getMainPoints(), bear.getEvidence(), bear.getConclusion(),
                neutral.getMainPoints(), neutral.getEvidence(), neutral.getConclusion());
    }

    /**
     * 构建辩论上下文（供 AnalysisController 调用）
     */
    public String buildDebateContext(String stockCode, String stockName, String industry) {
        return String.format("""
                待分析股票：%s（%s）
                所属行业：%s

                请从各自立场出发，对该股票进行深入分析，给出有说服力的论点。
                """, stockName, stockCode, industry);
    }
}
