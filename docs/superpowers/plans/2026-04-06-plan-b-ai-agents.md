# Plan B: AI Agent Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现多 LLM Agent 层，包含四 Agent 流水线（研究员→技术分析师→舆情分析师→投资经理）和三方辩论引擎（多方/空方/中立→仲裁官），接入 AnalysisController。

**Architecture:** 使用 LangChain4j AiService 模式定义 Agent 接口，各 Service 在 `@PostConstruct` 中绑定对应模型，PipelineService 顺序执行并发布 SSE 事件，DebateService 用 CompletableFuture 并行调用三方 Agent 后交由仲裁官裁决，AnalysisController 异步触发对应服务。

**Tech Stack:** Java 17, LangChain4j 0.36, Spring @Async, CompletableFuture, Jackson ObjectMapper

---

## 重要上下文（必读）

**工作目录:** `/Users/wuzhongheng/项目/stock-analysis-multi-agent/.worktrees/plan-b-agents`
**分支:** `feature/plan-b-agents`（基于 `feature/plan-a-backend`，已包含所有 Plan A 代码）

**已有的关键类（不要重复创建）：**
- `com.stockanalysis.model.stock.*` — StockInfo, DailyPrice, FinancialData, NewsItem
- `com.stockanalysis.model.report.*` — BasicResearchReport, TechnicalReport, SentimentReport, InvestmentDecision, DebateArgument, ArbitratorDecision, Stance(enum)
- `com.stockanalysis.model.event.AnalysisStage` — enum，含所有阶段常量
- `com.stockanalysis.model.event.AgentCompletedEvent`
- `com.stockanalysis.service.AnalysisSessionService` — `publishEvent(sessionId, stage, message, data)`
- `com.stockanalysis.data.TushareProvider` — `getStockInfo`, `getDailyPrices`, `getFinancials`
- `com.stockanalysis.data.EastMoneyProvider` — `getNews`
- `com.stockanalysis.indicator.TechnicalIndicatorService` — `calculate(prices)` → TechnicalReport
- `com.stockanalysis.web.AnalysisController` — 已有骨架，Task 6 修改

**LLM 分配：**
| Agent | 模型 Bean |
|-------|-----------|
| ResearcherAgent | GPT-4o（auto-configured primary） |
| TechnicalAnalystAgent | GPT-4o |
| SentimentAnalystAgent | DeepSeek（`deepSeekChatModel`） |
| InvestmentManagerAgent | GPT-4o |
| BullAgent | GPT-4o |
| BearAgent | Claude（`anthropicChatModel`） |
| NeutralAgent | DeepSeek（`deepSeekChatModel`） |
| ArbitratorAgent | GPT-4o |

---

## 文件结构

```
src/main/java/com/stockanalysis/
├── config/
│   └── AiModelConfig.java                    # Claude + DeepSeek 模型 Bean 配置
├── agent/
│   ├── pipeline/
│   │   ├── ResearcherAgentService.java       # LangChain4j AiService 接口
│   │   ├── TechnicalAnalystAgentService.java
│   │   ├── SentimentAnalystAgentService.java
│   │   └── InvestmentManagerAgentService.java
│   └── debate/
│       ├── BullAgentService.java
│       ├── BearAgentService.java
│       ├── NeutralAgentService.java
│       └── ArbitratorAgentService.java
├── service/
│   ├── LlmResponseParser.java                # Jackson JSON 解析工具
│   ├── PipelineService.java                  # 顺序流水线 + 事件发布
│   └── DebateService.java                    # 并行辩论 + 仲裁
└── web/
    └── AnalysisController.java               # 修改：接入 Pipeline/Debate 服务
```

---

## Task 1: AiModelConfig — 配置 Claude 和 DeepSeek 模型

**Files:**
- Create: `src/main/java/com/stockanalysis/config/AiModelConfig.java`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: 在 application.yml 中添加模型配置**

在 `app:` 节点下添加模型配置（`app.api-keys.anthropic` 和 `app.api-keys.deepseek` 已存在）：

```yaml
app:
  api-keys:
    anthropic: ${ANTHROPIC_API_KEY:}
    deepseek: ${DEEPSEEK_API_KEY:}
    tushare: ${TUSHARE_TOKEN:}
  models:
    claude:
      model-name: claude-sonnet-4-6
      max-tokens: 4096
      temperature: 0.3
    deepseek:
      base-url: https://api.deepseek.com/v1
      model-name: deepseek-chat
      temperature: 0.3
      timeout-seconds: 120
```

- [ ] **Step 2: 创建 AiModelConfig**

`src/main/java/com/stockanalysis/config/AiModelConfig.java`:

```java
package com.stockanalysis.config;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AiModelConfig {

    // GPT-4o 由 langchain4j-open-ai-spring-boot-starter 通过 application.yml 自动配置
    // bean 名称为 "openAiChatModel"，是 ChatLanguageModel 的 @Primary bean

    @Bean("anthropicChatModel")
    public ChatLanguageModel anthropicChatModel(
            @Value("${app.api-keys.anthropic}") String apiKey,
            @Value("${app.models.claude.model-name}") String modelName,
            @Value("${app.models.claude.max-tokens}") int maxTokens,
            @Value("${app.models.claude.temperature}") double temperature) {
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();
    }

    @Bean("deepSeekChatModel")
    public ChatLanguageModel deepSeekChatModel(
            @Value("${app.api-keys.deepseek}") String apiKey,
            @Value("${app.models.deepseek.base-url}") String baseUrl,
            @Value("${app.models.deepseek.model-name}") String modelName,
            @Value("${app.models.deepseek.temperature}") double temperature,
            @Value("${app.models.deepseek.timeout-seconds}") int timeoutSeconds) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }
}
```

- [ ] **Step 3: 验证编译**

```bash
cd /Users/wuzhongheng/项目/stock-analysis-multi-agent/.worktrees/plan-b-agents
mvn compile -q
```

期望：`BUILD SUCCESS`

若出现 `AnthropicChatModel` 找不到的错误，检查 `langchain4j-anthropic-spring-boot-starter` 的实际包路径：
```bash
jar tf ~/.m2/repository/dev/langchain4j/langchain4j-anthropic/0.36.0/langchain4j-anthropic-0.36.0.jar | grep AnthropicChatModel
```
根据实际包路径修正 import。

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/stockanalysis/config/AiModelConfig.java src/main/resources/application.yml
git commit -m "feat: configure Claude and DeepSeek AI model beans"
```

---

## Task 2: LlmResponseParser — JSON 解析工具

**Files:**
- Create: `src/main/java/com/stockanalysis/service/LlmResponseParser.java`
- Create: `src/test/java/com/stockanalysis/service/LlmResponseParserTest.java`

- [ ] **Step 1: 写失败测试**

`src/test/java/com/stockanalysis/service/LlmResponseParserTest.java`:

```java
package com.stockanalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmResponseParserTest {

    private final LlmResponseParser parser = new LlmResponseParser();

    @Test
    void extractJson_plainJson() {
        String response = "{\"rating\":\"买入\",\"confidence\":75}";
        JsonNode node = parser.extractJson(response);
        assertThat(node.get("rating").asText()).isEqualTo("买入");
        assertThat(node.get("confidence").asInt()).isEqualTo(75);
    }

    @Test
    void extractJson_jsonInMarkdownBlock() {
        String response = "分析结果如下：\n```json\n{\"rating\":\"持有\"}\n```";
        JsonNode node = parser.extractJson(response);
        assertThat(node.get("rating").asText()).isEqualTo("持有");
    }

    @Test
    void extractJson_jsonEmbeddedInText() {
        String response = "根据分析，{\"rating\":\"卖出\",\"score\":30} 这是我的结论。";
        JsonNode node = parser.extractJson(response);
        assertThat(node.get("rating").asText()).isEqualTo("卖出");
    }

    @Test
    void getString_returnsFieldValue() {
        String response = "{\"analysisText\":\"股票表现良好\"}";
        String text = parser.getString(response, "analysisText", "默认值");
        assertThat(text).isEqualTo("股票表现良好");
    }

    @Test
    void getString_returnsDefaultOnMissingField() {
        String response = "{\"other\":\"value\"}";
        String text = parser.getString(response, "analysisText", "默认值");
        assertThat(text).isEqualTo("默认值");
    }

    @Test
    void getDouble_returnsNumericValue() {
        String response = "{\"sentimentScore\":0.75}";
        double score = parser.getDouble(response, "sentimentScore", 0.0);
        assertThat(score).isEqualTo(0.75);
    }

    @Test
    void getInt_returnsIntValue() {
        String response = "{\"confidencePercent\":80}";
        int confidence = parser.getInt(response, "confidencePercent", 50);
        assertThat(confidence).isEqualTo(80);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn test -Dtest=LlmResponseParserTest -Dgroups='!integration' 2>&1 | tail -5
```

期望：`FAIL` — LlmResponseParser 未创建

- [ ] **Step 3: 创建 LlmResponseParser**

`src/main/java/com/stockanalysis/service/LlmResponseParser.java`:

```java
package com.stockanalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class LlmResponseParser {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```");
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*\\}");

    /**
     * 从 LLM 响应中提取 JSON 节点。
     * 依次尝试：直接解析 → markdown 代码块 → 文本中第一个 JSON 对象
     */
    public JsonNode extractJson(String response) {
        if (response == null || response.isBlank()) {
            return objectMapper.createObjectNode();
        }
        // 1. 直接解析
        try {
            return objectMapper.readTree(response.trim());
        } catch (Exception ignored) {}

        // 2. 从 markdown 代码块提取
        Matcher blockMatcher = JSON_BLOCK.matcher(response);
        if (blockMatcher.find()) {
            try {
                return objectMapper.readTree(blockMatcher.group(1).trim());
            } catch (Exception ignored) {}
        }

        // 3. 从文本中提取第一个 JSON 对象
        Matcher objMatcher = JSON_OBJECT.matcher(response);
        while (objMatcher.find()) {
            try {
                return objectMapper.readTree(objMatcher.group());
            } catch (Exception ignored) {}
        }

        log.warn("Could not extract JSON from LLM response, returning empty node. Response: {}",
                response.length() > 200 ? response.substring(0, 200) + "..." : response);
        return objectMapper.createObjectNode();
    }

    public String getString(String response, String field, String defaultValue) {
        JsonNode node = extractJson(response);
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return defaultValue;
        return fieldNode.asText(defaultValue);
    }

    public double getDouble(String response, String field, double defaultValue) {
        JsonNode node = extractJson(response);
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return defaultValue;
        return fieldNode.asDouble(defaultValue);
    }

    public int getInt(String response, String field, int defaultValue) {
        JsonNode node = extractJson(response);
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return defaultValue;
        return fieldNode.asInt(defaultValue);
    }

    public boolean getBoolean(String response, String field, boolean defaultValue) {
        JsonNode node = extractJson(response);
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return defaultValue;
        return fieldNode.asBoolean(defaultValue);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
mvn test -Dtest=LlmResponseParserTest -Dgroups='!integration' 2>&1 | grep -E "Tests run|BUILD"
```

期望：`Tests run: 7, Failures: 0, Errors: 0` + `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/stockanalysis/service/LlmResponseParser.java \
        src/test/java/com/stockanalysis/service/LlmResponseParserTest.java
git commit -m "feat: add LlmResponseParser for robust JSON extraction from LLM responses"
```

---

## Task 3: Pipeline Agent 接口

**Files:**
- Create: `src/main/java/com/stockanalysis/agent/pipeline/ResearcherAgentService.java`
- Create: `src/main/java/com/stockanalysis/agent/pipeline/TechnicalAnalystAgentService.java`
- Create: `src/main/java/com/stockanalysis/agent/pipeline/SentimentAnalystAgentService.java`
- Create: `src/main/java/com/stockanalysis/agent/pipeline/InvestmentManagerAgentService.java`

- [ ] **Step 1: 创建 ResearcherAgentService**

`src/main/java/com/stockanalysis/agent/pipeline/ResearcherAgentService.java`:

```java
package com.stockanalysis.agent.pipeline;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ResearcherAgentService {

    @SystemMessage("""
            你是一位专业的A股股票基本面研究员，擅长分析上市公司财务状况和行业地位。
            根据提供的股票数据，进行深入的基本面分析。
            
            必须严格按以下JSON格式返回，不要包含任何其他内容：
            {
              "fundamentalRating": "优秀",
              "analysisText": "基本面详细分析文本（200-400字，分析PE/PB/ROE等指标并给出解读）"
            }
            
            fundamentalRating 只能是以下之一：优秀/良好/一般/较差
            """)
    String analyze(@UserMessage String stockContext);
}
```

- [ ] **Step 2: 创建 TechnicalAnalystAgentService**

`src/main/java/com/stockanalysis/agent/pipeline/TechnicalAnalystAgentService.java`:

```java
package com.stockanalysis.agent.pipeline;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface TechnicalAnalystAgentService {

    @SystemMessage("""
            你是一位专业的A股技术分析师，擅长K线形态、均线系统和技术指标分析。
            根据提供的技术指标数据，进行专业的技术面分析。
            
            必须严格按以下JSON格式返回，不要包含任何其他内容：
            {
              "technicalRating": "强势",
              "analysisText": "技术面详细分析文本（200-400字，分析趋势、均线、MACD、RSI、布林带等）"
            }
            
            technicalRating 只能是以下之一：强势/中性/弱势
            """)
    String analyze(@UserMessage String technicalContext);
}
```

- [ ] **Step 3: 创建 SentimentAnalystAgentService**

`src/main/java/com/stockanalysis/agent/pipeline/SentimentAnalystAgentService.java`:

```java
package com.stockanalysis.agent.pipeline;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface SentimentAnalystAgentService {

    @SystemMessage("""
            你是一位专业的A股舆情分析师，擅长中文财经新闻的情感分析和市场情绪判断。
            根据提供的新闻数据，分析市场舆情和投资者情绪。
            
            必须严格按以下JSON格式返回，不要包含任何其他内容：
            {
              "sentimentScore": 0.5,
              "sentimentLabel": "乐观",
              "keyPoints": ["关键点1", "关键点2", "关键点3"],
              "analysisText": "舆情详细分析文本（150-300字）"
            }
            
            sentimentScore：-1.0（极度悲观）到 1.0（极度乐观）之间的小数
            sentimentLabel：只能是 极度乐观/乐观/中性/悲观/极度悲观 之一
            keyPoints：3-5条关键新闻要点
            """)
    String analyze(@UserMessage String sentimentContext);
}
```

- [ ] **Step 4: 创建 InvestmentManagerAgentService**

`src/main/java/com/stockanalysis/agent/pipeline/InvestmentManagerAgentService.java`:

```java
package com.stockanalysis.agent.pipeline;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface InvestmentManagerAgentService {

    @SystemMessage("""
            你是一位资深A股投资经理，负责综合各类分析报告并给出最终投资建议。
            根据提供的基本面、技术面、舆情三份报告，给出综合投资决策。
            
            必须严格按以下JSON格式返回，不要包含任何其他内容：
            {
              "rating": "买入",
              "targetPriceLow": 1650.0,
              "targetPriceHigh": 1720.0,
              "confidencePercent": 72,
              "coreLogic": "核心投资逻辑（3-5条，每条一句话，用\\n分隔）",
              "mainRisks": "主要风险（3-5条，每条一句话，用\\n分隔）",
              "summaryText": "综合投资建议摘要（200-400字）"
            }
            
            rating 只能是以下之一：强烈买入/买入/持有/卖出/强烈卖出
            confidencePercent：0-100之间的整数
            targetPriceLow/targetPriceHigh：基于当前股价的合理目标价区间
            """)
    String decide(@UserMessage String allReports);
}
```

- [ ] **Step 5: 验证编译**

```bash
mvn compile -q
```

期望：`BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/stockanalysis/agent/pipeline/
git commit -m "feat: add pipeline agent interfaces (Researcher/Technical/Sentiment/InvestmentManager)"
```

---

## Task 4: PipelineService — 顺序流水线执行

**Files:**
- Create: `src/main/java/com/stockanalysis/service/PipelineService.java`

- [ ] **Step 1: 创建 PipelineService**

`src/main/java/com/stockanalysis/service/PipelineService.java`:

```java
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
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile -q
```

期望：`BUILD SUCCESS`。若出现 `AiServices` 找不到，检查 LangChain4j 实际导入路径：
```bash
jar tf ~/.m2/repository/dev/langchain4j/langchain4j/0.36.0/langchain4j-0.36.0.jar | grep AiServices
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/stockanalysis/service/PipelineService.java
git commit -m "feat: add PipelineService with 4-stage sequential agent pipeline"
```

---

## Task 5: Debate Agent 接口

**Files:**
- Create: `src/main/java/com/stockanalysis/agent/debate/BullAgentService.java`
- Create: `src/main/java/com/stockanalysis/agent/debate/BearAgentService.java`
- Create: `src/main/java/com/stockanalysis/agent/debate/NeutralAgentService.java`
- Create: `src/main/java/com/stockanalysis/agent/debate/ArbitratorAgentService.java`

- [ ] **Step 1: 创建辩论 Agent 接口**

`src/main/java/com/stockanalysis/agent/debate/BullAgentService.java`:

```java
package com.stockanalysis.agent.debate;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface BullAgentService {

    @SystemMessage("""
            你是一位激进的A股多头分析师。你的使命是从看多角度深入分析股票，寻找所有支持上涨的理由和证据。
            你只关注利好因素，并以最有力的方式呈现看多逻辑。
            
            必须严格按以下JSON格式返回，不要包含任何其他内容：
            {
              "mainPoints": "多方核心论点（3-5条理由，每条一句话，用\\n分隔）",
              "evidence": "支撑证据（引用数据、行业趋势、催化剂等）",
              "conclusion": "看多结论（2-3句话，包含预期涨幅）"
            }
            """)
    String argue(@UserMessage String stockContext);
}
```

`src/main/java/com/stockanalysis/agent/debate/BearAgentService.java`:

```java
package com.stockanalysis.agent.debate;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface BearAgentService {

    @SystemMessage("""
            你是一位严谨的A股空头分析师。你的使命是从看空角度深入分析股票，揭示所有风险和下跌理由。
            你善于发现潜在问题、高估泡沫和下行风险，以最有力的方式呈现看空逻辑。
            
            必须严格按以下JSON格式返回，不要包含任何其他内容：
            {
              "mainPoints": "空方核心论点（3-5条理由，每条一句话，用\\n分隔）",
              "evidence": "支撑证据（引用数据、风险因素、负面信号等）",
              "conclusion": "看空结论（2-3句话，包含预期跌幅或风险敞口）"
            }
            """)
    String argue(@UserMessage String stockContext);
}
```

`src/main/java/com/stockanalysis/agent/debate/NeutralAgentService.java`:

```java
package com.stockanalysis.agent.debate;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface NeutralAgentService {

    @SystemMessage("""
            你是一位独立的A股中立分析师。你不偏多也不偏空，客观呈现多空两方面的事实，评估不确定性。
            你关注数据本身，指出多空双方都可能忽视的关键因素。
            
            必须严格按以下JSON格式返回，不要包含任何其他内容：
            {
              "mainPoints": "中立核心观点（3-5条客观评估，每条一句话，用\\n分隔）",
              "evidence": "关键数据和不确定因素（列举多空双方都应关注的事实）",
              "conclusion": "中立结论（2-3句话，说明当前状态和主要不确定性）"
            }
            """)
    String argue(@UserMessage String stockContext);
}
```

`src/main/java/com/stockanalysis/agent/debate/ArbitratorAgentService.java`:

```java
package com.stockanalysis.agent.debate;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ArbitratorAgentService {

    @SystemMessage("""
            你是一位资深仲裁官。你的职责是综合评估多方、空方、中立三方的辩论论点，识别各方逻辑的强弱，给出公正的最终裁决。
            你独立于三方，只关注论证质量和证据可靠性。
            
            必须严格按以下JSON格式返回，不要包含任何其他内容：
            {
              "finalRating": "持有",
              "confidencePercent": 65,
              "strongestArgument": "BULL",
              "weakestArgument": "BEAR",
              "synthesisText": "综合裁决全文（300-500字，分析三方论点并说明裁决依据）",
              "needsSecondRound": false,
              "secondRoundFocus": ""
            }
            
            finalRating 只能是：强烈买入/买入/持有/卖出/强烈卖出
            confidencePercent：0-100之间的整数
            strongestArgument / weakestArgument：只能是 BULL/BEAR/NEUTRAL 之一
            needsSecondRound：若三方分歧极大且关键争议点未解决，设为 true
            secondRoundFocus：若 needsSecondRound 为 true，说明第二轮需聚焦的争议点
            """)
    String adjudicate(@UserMessage String debateContext);
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile -q
```

期望：`BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/stockanalysis/agent/debate/
git commit -m "feat: add debate agent interfaces (Bull/Bear/Neutral/Arbitrator)"
```

---

## Task 6: DebateService — 并行辩论执行

**Files:**
- Create: `src/main/java/com/stockanalysis/service/DebateService.java`

- [ ] **Step 1: 创建 DebateService**

`src/main/java/com/stockanalysis/service/DebateService.java`:

```java
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

            String bullResponse   = bullFuture.get();
            String bearResponse   = bearFuture.get();
            String neutralResponse = neutralFuture.get();

            DebateArgument bullArg = parseArgument(bullResponse, Stance.BULL, "GPT-4o");
            DebateArgument bearArg = parseArgument(bearResponse, Stance.BEAR, "Claude");
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

            String arbitratorContext = buildArbitratorContext(bullArg, bearArg, neutralArg);
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
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile -q
```

期望：`BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/stockanalysis/service/DebateService.java
git commit -m "feat: add DebateService with parallel Bull/Bear/Neutral agents and Arbitrator"
```

---

## Task 7: Wire AnalysisController — 接入 Pipeline 和 Debate

**Files:**
- Modify: `src/main/java/com/stockanalysis/web/AnalysisController.java`

- [ ] **Step 1: 读取当前 AnalysisController 内容**

```bash
cat src/main/java/com/stockanalysis/web/AnalysisController.java
```

- [ ] **Step 2: 更新 AnalysisController**

用以下完整内容替换 `src/main/java/com/stockanalysis/web/AnalysisController.java`：

```java
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
```

- [ ] **Step 3: 验证编译**

```bash
mvn compile -q
```

期望：`BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/stockanalysis/web/AnalysisController.java
git commit -m "feat: wire AnalysisController to PipelineService and DebateService"
```

---

## Task 8: 集成验证

**验证应用可以正常启动，API 接口响应正确。**

- [ ] **Step 1: 检查所有单元测试通过**

```bash
mvn test -Dgroups='!integration' 2>&1 | grep -E "Tests run|BUILD"
```

期望：`Tests run: 9, Failures: 0, Errors: 0` + `BUILD SUCCESS`
（2 个 TechnicalIndicatorServiceTest + 7 个 LlmResponseParserTest）

- [ ] **Step 2: 启动应用（测试 API Key）**

```bash
OPENAI_API_KEY=test ANTHROPIC_API_KEY=test DEEPSEEK_API_KEY=test TUSHARE_TOKEN=test \
mvn spring-boot:run -q &
sleep 10

# 测试接口返回
curl -s -X POST http://localhost:8080/api/analysis/start \
  -H "Content-Type: application/json" \
  -d '{"stockCode":"600519.SH","mode":"pipeline","dateRange":"3m"}' | python3 -m json.tool

pkill -f "spring-boot:run" 2>/dev/null || true
```

期望输出：包含 `sessionId`、`status: started`、`mode: pipeline` 的 JSON

- [ ] **Step 3: Final Commit**

```bash
git add .
git commit -m "feat: Plan B complete - AI agent layer ready"
```

---

## 完成标志

Plan B 完成后具备：
- ✅ GPT-4o（auto-configured）+ Claude + DeepSeek 三模型配置
- ✅ LlmResponseParser（健壮 JSON 提取，7 个单元测试）
- ✅ 4 个流水线 Agent 接口（Researcher/Technical/Sentiment/InvestmentManager）
- ✅ PipelineService（@Async 顺序执行，发布 SSE 事件）
- ✅ 4 个辩论 Agent 接口（Bull/Bear/Neutral/Arbitrator）
- ✅ DebateService（CompletableFuture 并行，仲裁官综合裁决）
- ✅ AnalysisController 路由完整

**下一步：** Plan C（Vue 3 前端）
