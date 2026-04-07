package com.stockanalysis.service;

import com.stockanalysis.agent.pipeline.*;
import com.stockanalysis.data.EastMoneyProvider;
import com.stockanalysis.data.TushareProvider;
import com.stockanalysis.indicator.TechnicalIndicatorService;
import com.stockanalysis.model.event.AnalysisStage;
import com.stockanalysis.model.report.*;
import com.stockanalysis.model.stock.DailyPrice;
import com.stockanalysis.model.stock.FinancialData;
import com.stockanalysis.model.stock.NewsItem;
import com.stockanalysis.model.stock.StockInfo;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PipelineService {

    private final ChatLanguageModel gpt4oModel;
    private final ChatLanguageModel deepSeekModel;
    private final TushareProvider tushareProvider;
    private final EastMoneyProvider eastMoneyProvider;
    private final TechnicalIndicatorService indicatorService;
    private final AnalysisSessionService sessionService;
    private final LlmResponseParser parser;

    private ResearcherAgentService researcherAgent;
    private TechnicalAnalystAgentService technicalAgent;
    private SentimentAnalystAgentService sentimentAgent;
    private InvestmentManagerAgentService investmentManagerAgent;

    public PipelineService(
            ChatLanguageModel gpt4oModel,
            @Qualifier("deepSeekChatModel") ChatLanguageModel deepSeekModel,
            TushareProvider tushareProvider,
            EastMoneyProvider eastMoneyProvider,
            TechnicalIndicatorService indicatorService,
            AnalysisSessionService sessionService,
            LlmResponseParser parser) {
        this.gpt4oModel = gpt4oModel;
        this.deepSeekModel = deepSeekModel;
        this.tushareProvider = tushareProvider;
        this.eastMoneyProvider = eastMoneyProvider;
        this.indicatorService = indicatorService;
        this.sessionService = sessionService;
        this.parser = parser;
    }

    @PostConstruct
    void initAgents() {
        researcherAgent = AiServices.builder(ResearcherAgentService.class)
                .chatLanguageModel(gpt4oModel).build();
        technicalAgent = AiServices.builder(TechnicalAnalystAgentService.class)
                .chatLanguageModel(gpt4oModel).build();
        sentimentAgent = AiServices.builder(SentimentAnalystAgentService.class)
                .chatLanguageModel(deepSeekModel).build();
        investmentManagerAgent = AiServices.builder(InvestmentManagerAgentService.class)
                .chatLanguageModel(gpt4oModel).build();
        log.info("Pipeline agents initialized");
    }

    @Async
    public void runPipeline(String sessionId, String stockCode, String dateRange) {
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = switch (dateRange) {
                case "1m" -> endDate.minusMonths(1);
                case "6m" -> endDate.minusMonths(6);
                case "1y" -> endDate.minusYears(1);
                default   -> endDate.minusMonths(3); // "3m" default
            };

            // === Stage 1: Researcher ===
            sessionService.publishEvent(sessionId, AnalysisStage.RESEARCHER_STARTED,
                    "研究员正在收集基本面数据...", null);

            StockInfo stockInfo = tushareProvider.getStockInfo(stockCode);
            FinancialData financialData = tushareProvider.getFinancials(stockCode);
            String researchContext = buildResearchContext(stockInfo, financialData);
            String researchResponse = researcherAgent.analyze(researchContext);

            BasicResearchReport researchReport = new BasicResearchReport();
            researchReport.setStockInfo(stockInfo);
            researchReport.setFinancialData(financialData);
            researchReport.setFundamentalRating(parser.getString(researchResponse, "fundamentalRating", "一般"));
            researchReport.setAnalysisText(parser.getString(researchResponse, "analysisText", researchResponse));

            sessionService.publishEvent(sessionId, AnalysisStage.RESEARCHER_COMPLETED,
                    "研究员完成基本面分析：" + researchReport.getFundamentalRating(), researchReport);

            // === Stage 2: Technical Analyst ===
            sessionService.publishEvent(sessionId, AnalysisStage.TECHNICAL_STARTED,
                    "技术分析师正在分析K线和指标...", null);

            List<DailyPrice> prices = tushareProvider.getDailyPrices(stockCode, startDate, endDate);
            TechnicalReport techReport = indicatorService.calculate(prices);
            String techContext = buildTechnicalContext(stockInfo, techReport, prices);
            String techResponse = technicalAgent.analyze(techContext);

            techReport.setTechnicalRating(parser.getString(techResponse, "technicalRating", "中性"));
            techReport.setAnalysisText(parser.getString(techResponse, "analysisText", techResponse));

            sessionService.publishEvent(sessionId, AnalysisStage.TECHNICAL_COMPLETED,
                    "技术分析师完成分析：" + techReport.getTrendDirection() + "，" + techReport.getTechnicalRating(), techReport);

            // === Stage 3: Sentiment Analyst ===
            sessionService.publishEvent(sessionId, AnalysisStage.SENTIMENT_STARTED,
                    "舆情分析师正在分析新闻和市场情绪...", null);

            List<NewsItem> news = eastMoneyProvider.getNews(stockCode, 10);
            String sentimentContext = buildSentimentContext(stockInfo, news);
            String sentimentResponse = sentimentAgent.analyze(sentimentContext);

            SentimentReport sentimentReport = new SentimentReport();
            sentimentReport.setSentimentScore(parser.getDouble(sentimentResponse, "sentimentScore", 0.0));
            sentimentReport.setSentimentLabel(parser.getString(sentimentResponse, "sentimentLabel", "中性"));
            sentimentReport.setAnalysisText(parser.getString(sentimentResponse, "analysisText", sentimentResponse));
            // 解析 keyPoints 数组
            try {
                com.fasterxml.jackson.databind.JsonNode node = parser.extractJson(sentimentResponse);
                com.fasterxml.jackson.databind.JsonNode kp = node.get("keyPoints");
                if (kp != null && kp.isArray()) {
                    List<String> keyPoints = new java.util.ArrayList<>();
                    kp.forEach(p -> keyPoints.add(p.asText()));
                    sentimentReport.setKeyNewsPoints(keyPoints);
                }
            } catch (Exception e) {
                log.warn("Failed to parse keyPoints from sentiment response", e);
            }

            sessionService.publishEvent(sessionId, AnalysisStage.SENTIMENT_COMPLETED,
                    "舆情分析师完成分析：情感得分 " + String.format("%.2f", sentimentReport.getSentimentScore()), sentimentReport);

            // === Stage 4: Investment Manager ===
            sessionService.publishEvent(sessionId, AnalysisStage.INVESTMENT_MANAGER_STARTED,
                    "投资经理正在综合评估，给出投资建议...", null);

            String allReports = buildInvestmentContext(stockInfo, researchReport, techReport, sentimentReport);
            String decisionResponse = investmentManagerAgent.decide(allReports);

            InvestmentDecision decision = new InvestmentDecision();
            decision.setRating(parser.getString(decisionResponse, "rating", "持有"));
            decision.setTargetPriceLow(BigDecimal.valueOf(parser.getDouble(decisionResponse, "targetPriceLow", 0.0)));
            decision.setTargetPriceHigh(BigDecimal.valueOf(parser.getDouble(decisionResponse, "targetPriceHigh", 0.0)));
            decision.setConfidencePercent(parser.getInt(decisionResponse, "confidencePercent", 50));
            decision.setCoreLogic(parser.getString(decisionResponse, "coreLogic", ""));
            decision.setMainRisks(parser.getString(decisionResponse, "mainRisks", ""));
            decision.setSummaryText(parser.getString(decisionResponse, "summaryText", decisionResponse));

            sessionService.publishEvent(sessionId, AnalysisStage.INVESTMENT_MANAGER_COMPLETED,
                    "投资经理裁定：" + decision.getRating() + "（置信度 " + decision.getConfidencePercent() + "%）", decision);
            sessionService.publishEvent(sessionId, AnalysisStage.ANALYSIS_COMPLETED,
                    "流水线分析完成", decision);

        } catch (Exception e) {
            log.error("Pipeline failed for session {} stock {}", sessionId, stockCode, e);
            sessionService.publishEvent(sessionId, AnalysisStage.ANALYSIS_FAILED,
                    "分析失败：" + e.getMessage(), null);
        }
    }

    private String buildResearchContext(StockInfo info, FinancialData fd) {
        return String.format("""
                股票代码：%s
                股票名称：%s
                所属行业：%s

                最新财务数据（报告期：%s）：
                - 市盈率(PE)：%s
                - 市净率(PB)：%s
                - 净资产收益率(ROE)：%s%%
                - 资产负债率：%s%%
                - 总市值：%s亿元
                """,
                info.getCode(), info.getName(), info.getIndustry(),
                fd.getReportPeriod() != null ? fd.getReportPeriod() : "最新",
                fd.getPe(), fd.getPb(), fd.getRoe(), fd.getDebtRatio(), fd.getTotalMarketCap());
    }

    private String buildTechnicalContext(StockInfo info, TechnicalReport report, List<DailyPrice> prices) {
        BigDecimal latestClose = prices.isEmpty() ? BigDecimal.ZERO : prices.get(prices.size() - 1).getClose();
        return String.format("""
                股票：%s（%s）
                最新收盘价：%s

                均线系统：
                - MA5：%s | MA20：%s | MA60：%s
                - 趋势方向：%s

                技术指标：
                - MACD：%s
                - RSI(14)：%s
                - 布林带上轨：%s | 中轨：%s | 下轨：%s

                支撑位：%s
                压力位：%s
                """,
                info.getName(), info.getCode(), latestClose,
                report.getMa5(), report.getMa20(), report.getMa60(), report.getTrendDirection(),
                report.getMacd(), report.getRsi14(),
                report.getBoll_upper(), report.getBoll_mid(), report.getBoll_lower(),
                report.getSupportLevel(), report.getResistanceLevel());
    }

    private String buildSentimentContext(StockInfo info, List<NewsItem> news) {
        String newsText = news.isEmpty() ? "暂无最新新闻" :
                news.stream()
                        .limit(8)
                        .map(n -> "- " + n.getTitle() + "（" + n.getPublishTime() + "）")
                        .collect(Collectors.joining("\n"));
        return String.format("""
                股票：%s（%s）行业：%s

                最新相关新闻（共%d条）：
                %s
                """,
                info.getName(), info.getCode(), info.getIndustry(),
                news.size(), newsText);
    }

    private String buildInvestmentContext(StockInfo info, BasicResearchReport research,
                                           TechnicalReport tech, SentimentReport sentiment) {
        return String.format("""
                综合分析报告 - %s（%s）

                【基本面分析】评级：%s
                %s

                【技术面分析】评级：%s，趋势：%s
                %s

                【舆情分析】情感：%s（得分：%.2f）
                %s
                """,
                info.getName(), info.getCode(),
                research.getFundamentalRating(), research.getAnalysisText(),
                tech.getTechnicalRating(), tech.getTrendDirection(), tech.getAnalysisText(),
                sentiment.getSentimentLabel(), sentiment.getSentimentScore(), sentiment.getAnalysisText());
    }
}
