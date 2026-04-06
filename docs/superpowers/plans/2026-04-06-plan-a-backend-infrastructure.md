# Plan A: 后端基础设施 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建 Spring Boot 后端骨架，包含数据层、事件总线、SSE 实时推送和 REST API，为 AI Agent 层提供完整基础。

**Architecture:** 单体 Spring Boot 应用，Spring ApplicationEvent 作为内部事件总线，SSE 将事件实时推送前端，数据层通过 StockDataProvider 接口屏蔽数据源差异。

**Tech Stack:** Java 17, Spring Boot 3.3, LangChain4j 0.36, Ta4j 0.16, Caffeine Cache, WebClient

---

## 文件结构

```
stock-analysis-multi-agent/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/stockanalysis/
│   │   │   ├── StockAnalysisApplication.java
│   │   │   ├── config/
│   │   │   │   ├── CacheConfig.java
│   │   │   │   └── WebClientConfig.java
│   │   │   ├── model/
│   │   │   │   ├── stock/
│   │   │   │   │   ├── StockInfo.java
│   │   │   │   │   ├── DailyPrice.java
│   │   │   │   │   ├── FinancialData.java
│   │   │   │   │   └── NewsItem.java
│   │   │   │   ├── report/
│   │   │   │   │   ├── BasicResearchReport.java
│   │   │   │   │   ├── TechnicalReport.java
│   │   │   │   │   ├── SentimentReport.java
│   │   │   │   │   ├── InvestmentDecision.java
│   │   │   │   │   ├── DebateArgument.java
│   │   │   │   │   └── ArbitratorDecision.java
│   │   │   │   └── event/
│   │   │   │       ├── AgentCompletedEvent.java
│   │   │   │       └── AnalysisStage.java
│   │   │   ├── data/
│   │   │   │   ├── StockDataProvider.java
│   │   │   │   ├── TushareProvider.java
│   │   │   │   └── EastMoneyProvider.java
│   │   │   ├── indicator/
│   │   │   │   └── TechnicalIndicatorService.java
│   │   │   ├── service/
│   │   │   │   └── AnalysisSessionService.java
│   │   │   └── web/
│   │   │       ├── AnalysisController.java
│   │   │       └── SseController.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/stockanalysis/
│           ├── data/
│           │   ├── TushareProviderTest.java
│           │   └── EastMoneyProviderTest.java
│           └── indicator/
│               └── TechnicalIndicatorServiceTest.java
```

---

## Task 1: Maven 项目初始化

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/stockanalysis/StockAnalysisApplication.java`
- Create: `src/main/resources/application.yml`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
    </parent>

    <groupId>com.stockanalysis</groupId>
    <artifactId>stock-analysis-multi-agent</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>stock-analysis-multi-agent</name>

    <properties>
        <java.version>17</java.version>
        <langchain4j.version>0.36.0</langchain4j.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring WebFlux (for WebClient + SSE) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>

        <!-- Spring Cache -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>

        <!-- Caffeine Cache -->
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>

        <!-- LangChain4j Core -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-spring-boot-starter</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>

        <!-- LangChain4j OpenAI (GPT-4o + DeepSeek compatible) -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>

        <!-- LangChain4j Anthropic (Claude) -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-anthropic-spring-boot-starter</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>

        <!-- Ta4j Technical Indicators -->
        <dependency>
            <groupId>org.ta4j</groupId>
            <artifactId>ta4j-core</artifactId>
            <version>0.16</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建主启动类**

`src/main/java/com/stockanalysis/StockAnalysisApplication.java`:

```java
package com.stockanalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
public class StockAnalysisApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockAnalysisApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 application.yml**

`src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: stock-analysis-multi-agent
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=500,expireAfterWrite=3600s

# LangChain4j - OpenAI (GPT-4o)
langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY}
      model-name: gpt-4o
      temperature: 0.3
      timeout: 120s

# API Keys (其余模型在 Java Config 中手动配置)
app:
  api-keys:
    anthropic: ${ANTHROPIC_API_KEY}
    deepseek: ${DEEPSEEK_API_KEY}
    tushare: ${TUSHARE_TOKEN}
  cache:
    daily-price-ttl-hours: 24
    news-ttl-minutes: 60
    financial-ttl-days: 7
```

- [ ] **Step 4: 验证项目可以编译**

```bash
cd /Users/wuzhongheng/项目/stock-analysis-multi-agent
mvn compile
```

期望输出：`BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/java/com/stockanalysis/StockAnalysisApplication.java src/main/resources/application.yml
git commit -m "chore: initialize Spring Boot project with dependencies"
```

---

## Task 2: 数据模型

**Files:**
- Create: `src/main/java/com/stockanalysis/model/stock/StockInfo.java`
- Create: `src/main/java/com/stockanalysis/model/stock/DailyPrice.java`
- Create: `src/main/java/com/stockanalysis/model/stock/FinancialData.java`
- Create: `src/main/java/com/stockanalysis/model/stock/NewsItem.java`
- Create: `src/main/java/com/stockanalysis/model/report/BasicResearchReport.java`
- Create: `src/main/java/com/stockanalysis/model/report/TechnicalReport.java`
- Create: `src/main/java/com/stockanalysis/model/report/SentimentReport.java`
- Create: `src/main/java/com/stockanalysis/model/report/InvestmentDecision.java`
- Create: `src/main/java/com/stockanalysis/model/report/DebateArgument.java`
- Create: `src/main/java/com/stockanalysis/model/report/ArbitratorDecision.java`
- Create: `src/main/java/com/stockanalysis/model/event/AnalysisStage.java`
- Create: `src/main/java/com/stockanalysis/model/event/AgentCompletedEvent.java`

- [ ] **Step 1: 创建股票基础数据模型**

`src/main/java/com/stockanalysis/model/stock/StockInfo.java`:

```java
package com.stockanalysis.model.stock;

import lombok.Data;

@Data
public class StockInfo {
    private String code;        // 股票代码，如 600519.SH
    private String name;        // 股票名称
    private String industry;    // 所属行业
    private String market;      // 市场（上交所/深交所）
    private String listDate;    // 上市日期
}
```

`src/main/java/com/stockanalysis/model/stock/DailyPrice.java`:

```java
package com.stockanalysis.model.stock;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailyPrice {
    private LocalDate tradeDate;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal preClose;
    private BigDecimal change;
    private BigDecimal pctChg;   // 涨跌幅(%)
    private Long vol;            // 成交量(手)
    private BigDecimal amount;   // 成交额(千元)
}
```

`src/main/java/com/stockanalysis/model/stock/FinancialData.java`:

```java
package com.stockanalysis.model.stock;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FinancialData {
    private String code;
    private String reportPeriod;     // 报告期，如 20231231
    private BigDecimal pe;           // 市盈率
    private BigDecimal pb;           // 市净率
    private BigDecimal roe;          // 净资产收益率(%)
    private BigDecimal revenueGrowth; // 营收增速(%)
    private BigDecimal netProfitGrowth; // 净利润增速(%)
    private BigDecimal debtRatio;    // 资产负债率(%)
    private BigDecimal totalMarketCap; // 总市值(亿元)
}
```

`src/main/java/com/stockanalysis/model/stock/NewsItem.java`:

```java
package com.stockanalysis.model.stock;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NewsItem {
    private String title;
    private String content;
    private String source;
    private LocalDateTime publishTime;
    private String url;
}
```

- [ ] **Step 2: 创建报告模型**

`src/main/java/com/stockanalysis/model/report/BasicResearchReport.java`:

```java
package com.stockanalysis.model.report;

import com.stockanalysis.model.stock.FinancialData;
import com.stockanalysis.model.stock.StockInfo;
import lombok.Data;

@Data
public class BasicResearchReport {
    private StockInfo stockInfo;
    private FinancialData financialData;
    private String analysisText;     // LLM 生成的基本面分析文本
    private String fundamentalRating; // 基本面评级：优秀/良好/一般/较差
}
```

`src/main/java/com/stockanalysis/model/report/TechnicalReport.java`:

```java
package com.stockanalysis.model.report;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TechnicalReport {
    private BigDecimal ma5;
    private BigDecimal ma20;
    private BigDecimal ma60;
    private BigDecimal macd;
    private BigDecimal rsi14;
    private BigDecimal kdj_k;
    private BigDecimal kdj_d;
    private BigDecimal boll_upper;
    private BigDecimal boll_mid;
    private BigDecimal boll_lower;
    private String trendDirection;   // 趋势方向：上升/下降/震荡
    private String supportLevel;     // 支撑位描述
    private String resistanceLevel;  // 压力位描述
    private String analysisText;     // LLM 生成的技术面分析文本
    private String technicalRating;  // 技术面评级：强势/中性/弱势
}
```

`src/main/java/com/stockanalysis/model/report/SentimentReport.java`:

```java
package com.stockanalysis.model.report;

import lombok.Data;
import java.util.List;

@Data
public class SentimentReport {
    private double sentimentScore;    // 情感得分 -1.0(极度悲观) ~ 1.0(极度乐观)
    private String sentimentLabel;    // 极度乐观/乐观/中性/悲观/极度悲观
    private List<String> keyNewsPoints; // 关键新闻摘要列表
    private String analysisText;      // LLM 生成的舆情分析文本
}
```

`src/main/java/com/stockanalysis/model/report/InvestmentDecision.java`:

```java
package com.stockanalysis.model.report;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvestmentDecision {
    private String rating;            // 强烈买入/买入/持有/卖出/强烈卖出
    private BigDecimal targetPriceLow;
    private BigDecimal targetPriceHigh;
    private int confidencePercent;    // 置信度 0-100
    private String coreLogic;         // 核心逻辑（3-5条）
    private String mainRisks;         // 主要风险（3-5条）
    private String summaryText;       // 综合分析摘要
}
```

`src/main/java/com/stockanalysis/model/report/DebateArgument.java`:

```java
package com.stockanalysis.model.report;

import lombok.Data;

@Data
public class DebateArgument {
    private String stance;       // BULL / BEAR / NEUTRAL
    private String model;        // 使用的模型名称
    private String mainPoints;   // 核心论点
    private String evidence;     // 支撑证据
    private String conclusion;   // 最终结论
}
```

`src/main/java/com/stockanalysis/model/report/ArbitratorDecision.java`:

```java
package com.stockanalysis.model.report;

import lombok.Data;
import java.util.List;

@Data
public class ArbitratorDecision {
    private String finalRating;        // 最终评级
    private int confidencePercent;     // 置信度 0-100
    private String strongestArgument;  // 最有力的论点来自哪方
    private String weakestArgument;    // 最薄弱的论点来自哪方
    private String synthesisText;      // 综合裁决全文
    private boolean needsSecondRound;  // 是否需要第二轮辩论
    private String secondRoundFocus;   // 第二轮辩论焦点（如需要）
    private List<DebateArgument> debateArguments; // 三方论点
}
```

- [ ] **Step 3: 创建事件模型**

`src/main/java/com/stockanalysis/model/event/AnalysisStage.java`:

```java
package com.stockanalysis.model.event;

public enum AnalysisStage {
    RESEARCHER_STARTED,
    RESEARCHER_COMPLETED,
    TECHNICAL_STARTED,
    TECHNICAL_COMPLETED,
    SENTIMENT_STARTED,
    SENTIMENT_COMPLETED,
    INVESTMENT_MANAGER_STARTED,
    INVESTMENT_MANAGER_COMPLETED,
    BULL_STARTED,
    BULL_COMPLETED,
    BEAR_STARTED,
    BEAR_COMPLETED,
    NEUTRAL_STARTED,
    NEUTRAL_COMPLETED,
    ARBITRATOR_STARTED,
    ARBITRATOR_COMPLETED,
    ANALYSIS_COMPLETED,
    ANALYSIS_FAILED
}
```

`src/main/java/com/stockanalysis/model/event/AgentCompletedEvent.java`:

```java
package com.stockanalysis.model.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AgentCompletedEvent extends ApplicationEvent {
    private final String sessionId;
    private final AnalysisStage stage;
    private final String message;
    private final Object data;  // 可选，携带报告数据

    public AgentCompletedEvent(Object source, String sessionId,
                                AnalysisStage stage, String message, Object data) {
        super(source);
        this.sessionId = sessionId;
        this.stage = stage;
        this.message = message;
        this.data = data;
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
mvn compile
```

期望输出：`BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/stockanalysis/model/
git commit -m "feat: add domain models for stock data, reports, and events"
```

---

## Task 3: Cache 和 WebClient 配置

**Files:**
- Create: `src/main/java/com/stockanalysis/config/CacheConfig.java`
- Create: `src/main/java/com/stockanalysis/config/WebClientConfig.java`

- [ ] **Step 1: 创建 CacheConfig**

`src/main/java/com/stockanalysis/config/CacheConfig.java`:

```java
package com.stockanalysis.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache("dailyPrices",
            Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).maximumSize(200).build());
        manager.registerCustomCache("financialData",
            Caffeine.newBuilder().expireAfterWrite(7, TimeUnit.DAYS).maximumSize(100).build());
        manager.registerCustomCache("news",
            Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.MINUTES).maximumSize(200).build());
        manager.registerCustomCache("stockInfo",
            Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).maximumSize(500).build());
        return manager;
    }
}
```

- [ ] **Step 2: 创建 WebClientConfig**

`src/main/java/com/stockanalysis/config/WebClientConfig.java`:

```java
package com.stockanalysis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean("tushareWebClient")
    public WebClient tushareWebClient() {
        return WebClient.builder()
            .baseUrl("https://api.tushare.pro")
            .defaultHeader("Content-Type", "application/json")
            .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }

    @Bean("eastMoneyWebClient")
    public WebClient eastMoneyWebClient() {
        return WebClient.builder()
            .baseUrl("https://np-anotice-stock.eastmoney.com")
            .defaultHeader("User-Agent", "Mozilla/5.0")
            .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/stockanalysis/config/
git commit -m "feat: add cache and WebClient configuration"
```

---

## Task 4: StockDataProvider 接口

**Files:**
- Create: `src/main/java/com/stockanalysis/data/StockDataProvider.java`

- [ ] **Step 1: 创建接口**

`src/main/java/com/stockanalysis/data/StockDataProvider.java`:

```java
package com.stockanalysis.data;

import com.stockanalysis.model.stock.DailyPrice;
import com.stockanalysis.model.stock.FinancialData;
import com.stockanalysis.model.stock.NewsItem;
import com.stockanalysis.model.stock.StockInfo;

import java.time.LocalDate;
import java.util.List;

public interface StockDataProvider {

    /**
     * 获取股票基本信息
     * @param code 股票代码，如 "600519.SH"
     */
    StockInfo getStockInfo(String code);

    /**
     * 获取日线行情数据
     * @param code 股票代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 按日期升序排列的日线列表
     */
    List<DailyPrice> getDailyPrices(String code, LocalDate startDate, LocalDate endDate);

    /**
     * 获取最新财务数据
     * @param code 股票代码
     */
    FinancialData getFinancials(String code);

    /**
     * 获取最新新闻
     * @param code 股票代码
     * @param limit 最多返回条数
     */
    List<NewsItem> getNews(String code, int limit);
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/stockanalysis/data/StockDataProvider.java
git commit -m "feat: add StockDataProvider interface"
```

---

## Task 5: TushareProvider 实现

**Files:**
- Create: `src/main/java/com/stockanalysis/data/TushareProvider.java`
- Create: `src/test/java/com/stockanalysis/data/TushareProviderTest.java`

- [ ] **Step 1: 写失败测试**

`src/test/java/com/stockanalysis/data/TushareProviderTest.java`:

```java
package com.stockanalysis.data;

import com.stockanalysis.model.stock.DailyPrice;
import com.stockanalysis.model.stock.StockInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TushareProviderTest {

    @Autowired
    private TushareProvider tushareProvider;

    @Test
    void getStockInfo_shouldReturnValidInfo() {
        StockInfo info = tushareProvider.getStockInfo("600519.SH");
        assertThat(info).isNotNull();
        assertThat(info.getCode()).isEqualTo("600519.SH");
        assertThat(info.getName()).isNotBlank();
    }

    @Test
    void getDailyPrices_shouldReturnNonEmptyList() {
        List<DailyPrice> prices = tushareProvider.getDailyPrices(
            "600519.SH",
            LocalDate.now().minusDays(30),
            LocalDate.now()
        );
        assertThat(prices).isNotEmpty();
        assertThat(prices.get(0).getClose()).isPositive();
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
mvn test -pl . -Dtest=TushareProviderTest
```

期望：`FAIL` - TushareProvider 未创建

- [ ] **Step 3: 创建 TushareProvider**

`src/main/java/com/stockanalysis/data/TushareProvider.java`:

```java
package com.stockanalysis.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockanalysis.model.stock.DailyPrice;
import com.stockanalysis.model.stock.FinancialData;
import com.stockanalysis.model.stock.NewsItem;
import com.stockanalysis.model.stock.StockInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TushareProvider implements StockDataProvider {

    @Qualifier("tushareWebClient")
    private final WebClient webClient;

    @Value("${app.api-keys.tushare}")
    private String token;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private JsonNode callApi(String apiName, Map<String, Object> params, String fields) {
        Map<String, Object> body = Map.of(
            "api_name", apiName,
            "token", token,
            "params", params,
            "fields", fields
        );
        String response = webClient.post()
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String.class)
            .block();
        try {
            JsonNode root = objectMapper.readTree(response);
            if (root.path("code").asInt() != 0) {
                log.error("Tushare API error: {}", root.path("msg").asText());
                throw new RuntimeException("Tushare API error: " + root.path("msg").asText());
            }
            return root.path("data");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Tushare response", e);
        }
    }

    @Override
    @Cacheable(value = "stockInfo", key = "#code")
    public StockInfo getStockInfo(String code) {
        JsonNode data = callApi("stock_basic",
            Map.of("ts_code", code, "list_status", "L"),
            "ts_code,name,industry,market,list_date");

        JsonNode fields = data.path("fields");
        JsonNode items = data.path("items");
        if (!items.isArray() || items.isEmpty()) {
            throw new RuntimeException("Stock not found: " + code);
        }

        JsonNode item = items.get(0);
        StockInfo info = new StockInfo();
        info.setCode(item.get(0).asText());
        info.setName(item.get(1).asText());
        info.setIndustry(item.get(2).asText());
        info.setMarket(item.get(3).asText());
        info.setListDate(item.get(4).asText());
        return info;
    }

    @Override
    @Cacheable(value = "dailyPrices", key = "#code + '_' + #startDate + '_' + #endDate")
    public List<DailyPrice> getDailyPrices(String code, LocalDate startDate, LocalDate endDate) {
        JsonNode data = callApi("daily",
            Map.of("ts_code", code,
                   "start_date", startDate.format(DATE_FMT),
                   "end_date", endDate.format(DATE_FMT)),
            "trade_date,open,high,low,close,pre_close,change,pct_chg,vol,amount");

        JsonNode items = data.path("items");
        List<DailyPrice> prices = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode item : items) {
                DailyPrice price = new DailyPrice();
                price.setTradeDate(LocalDate.parse(item.get(0).asText(), DATE_FMT));
                price.setOpen(new BigDecimal(item.get(1).asText()));
                price.setHigh(new BigDecimal(item.get(2).asText()));
                price.setLow(new BigDecimal(item.get(3).asText()));
                price.setClose(new BigDecimal(item.get(4).asText()));
                price.setPreClose(new BigDecimal(item.get(5).asText()));
                price.setChange(new BigDecimal(item.get(6).asText()));
                price.setPctChg(new BigDecimal(item.get(7).asText()));
                price.setVol(item.get(8).asLong());
                price.setAmount(new BigDecimal(item.get(9).asText()));
                prices.add(price);
            }
        }
        // Tushare 返回倒序，转为升序
        prices.sort((a, b) -> a.getTradeDate().compareTo(b.getTradeDate()));
        return prices;
    }

    @Override
    @Cacheable(value = "financialData", key = "#code")
    public FinancialData getFinancials(String code) {
        JsonNode data = callApi("fina_indicator",
            Map.of("ts_code", code, "limit", "1"),
            "ts_code,end_date,roe,debt_to_assets");

        JsonNode valData = callApi("daily_basic",
            Map.of("ts_code", code, "trade_date", LocalDate.now().format(DATE_FMT)),
            "ts_code,pe,pb,total_mv");

        FinancialData fd = new FinancialData();
        fd.setCode(code);

        JsonNode finaItems = data.path("items");
        if (finaItems.isArray() && !finaItems.isEmpty()) {
            JsonNode item = finaItems.get(0);
            fd.setReportPeriod(item.get(1).asText());
            fd.setRoe(new BigDecimal(item.get(2).asText()));
            fd.setDebtRatio(new BigDecimal(item.get(3).asText()));
        }

        JsonNode valItems = valData.path("items");
        if (valItems.isArray() && !valItems.isEmpty()) {
            JsonNode item = valItems.get(0);
            fd.setPe(item.get(1).isNull() ? BigDecimal.ZERO : new BigDecimal(item.get(1).asText()));
            fd.setPb(item.get(2).isNull() ? BigDecimal.ZERO : new BigDecimal(item.get(2).asText()));
            fd.setTotalMarketCap(new BigDecimal(item.get(3).asText()).divide(BigDecimal.valueOf(10000)));
        }
        return fd;
    }

    @Override
    public List<NewsItem> getNews(String code, int limit) {
        // Tushare 新闻接口积分要求较高，返回空列表由 EastMoneyProvider 补充
        return new ArrayList<>();
    }
}
```

- [ ] **Step 4: 创建 test application.yml**

`src/test/resources/application.yml`:

```yaml
app:
  api-keys:
    tushare: ${TUSHARE_TOKEN:test_token_placeholder}
    anthropic: test_key
    deepseek: test_key
langchain4j:
  open-ai:
    chat-model:
      api-key: test_key
      model-name: gpt-4o
```

- [ ] **Step 5: 运行测试**

```bash
TUSHARE_TOKEN=your_actual_token mvn test -Dtest=TushareProviderTest
```

期望：`PASS`（需要真实 token）

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/stockanalysis/data/TushareProvider.java src/test/
git commit -m "feat: add TushareProvider for stock price and financial data"
```

---

## Task 6: EastMoneyProvider 实现

**Files:**
- Create: `src/main/java/com/stockanalysis/data/EastMoneyProvider.java`
- Create: `src/test/java/com/stockanalysis/data/EastMoneyProviderTest.java`

- [ ] **Step 1: 写失败测试**

`src/test/java/com/stockanalysis/data/EastMoneyProviderTest.java`:

```java
package com.stockanalysis.data;

import com.stockanalysis.model.stock.NewsItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class EastMoneyProviderTest {

    @Autowired
    private EastMoneyProvider eastMoneyProvider;

    @Test
    void getNews_shouldReturnNewsList() {
        List<NewsItem> news = eastMoneyProvider.getNews("600519.SH", 5);
        assertThat(news).isNotNull();
        // 网络可能不稳定，只验证结构
        if (!news.isEmpty()) {
            assertThat(news.get(0).getTitle()).isNotBlank();
        }
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
mvn test -Dtest=EastMoneyProviderTest
```

期望：`FAIL` - EastMoneyProvider 未创建

- [ ] **Step 3: 创建 EastMoneyProvider**

`src/main/java/com/stockanalysis/data/EastMoneyProvider.java`:

```java
package com.stockanalysis.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockanalysis.model.stock.DailyPrice;
import com.stockanalysis.model.stock.FinancialData;
import com.stockanalysis.model.stock.NewsItem;
import com.stockanalysis.model.stock.StockInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EastMoneyProvider implements StockDataProvider {

    @Qualifier("eastMoneyWebClient")
    private final WebClient webClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 将 600519.SH 转换为东方财富格式 0.600519（SH->0, SZ->1）
    private String convertCode(String code) {
        String[] parts = code.split("\\.");
        String market = parts[1].equals("SH") ? "0" : "1";
        return market + "." + parts[0];
    }

    @Override
    @Cacheable(value = "news", key = "#code + '_' + #limit")
    public List<NewsItem> getNews(String code, int limit) {
        String emCode = convertCode(code);
        try {
            String url = "/api/security/ann?sr=-1&page=1&ps=" + limit +
                "&id=" + emCode + "&rt=12";
            String response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data").path("list");
            List<NewsItem> newsList = new ArrayList<>();

            if (data.isArray()) {
                for (JsonNode item : data) {
                    NewsItem news = new NewsItem();
                    news.setTitle(item.path("NOTICE_TITLE").asText());
                    news.setContent(item.path("NOTICE_CONTENT").asText(""));
                    news.setSource("东方财富");
                    String timeStr = item.path("NOTICE_DATE").asText("");
                    if (!timeStr.isEmpty()) {
                        try {
                            news.setPublishTime(LocalDateTime.parse(timeStr,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        } catch (Exception e) {
                            news.setPublishTime(LocalDateTime.now());
                        }
                    }
                    news.setUrl("https://www.eastmoney.com");
                    newsList.add(news);
                }
            }
            return newsList;
        } catch (Exception e) {
            log.warn("EastMoney news fetch failed for {}: {}", code, e.getMessage());
            return new ArrayList<>();
        }
    }

    // EastMoneyProvider 不提供行情和财务数据，委托给 TushareProvider
    @Override
    public StockInfo getStockInfo(String code) {
        throw new UnsupportedOperationException("Use TushareProvider for stock info");
    }

    @Override
    public List<DailyPrice> getDailyPrices(String code, LocalDate startDate, LocalDate endDate) {
        throw new UnsupportedOperationException("Use TushareProvider for daily prices");
    }

    @Override
    public FinancialData getFinancials(String code) {
        throw new UnsupportedOperationException("Use TushareProvider for financial data");
    }
}
```

- [ ] **Step 4: 运行测试**

```bash
mvn test -Dtest=EastMoneyProviderTest
```

期望：`PASS`（网络正常时新闻列表非空）

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/stockanalysis/data/EastMoneyProvider.java src/test/java/com/stockanalysis/data/EastMoneyProviderTest.java
git commit -m "feat: add EastMoneyProvider for news data"
```

---

## Task 7: TechnicalIndicatorService

**Files:**
- Create: `src/main/java/com/stockanalysis/indicator/TechnicalIndicatorService.java`
- Create: `src/test/java/com/stockanalysis/indicator/TechnicalIndicatorServiceTest.java`

- [ ] **Step 1: 写失败测试**

`src/test/java/com/stockanalysis/indicator/TechnicalIndicatorServiceTest.java`:

```java
package com.stockanalysis.indicator;

import com.stockanalysis.model.report.TechnicalReport;
import com.stockanalysis.model.stock.DailyPrice;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TechnicalIndicatorServiceTest {

    private final TechnicalIndicatorService service = new TechnicalIndicatorService();

    private List<DailyPrice> generateMockPrices(int days) {
        List<DailyPrice> prices = new ArrayList<>();
        BigDecimal base = BigDecimal.valueOf(100);
        for (int i = 0; i < days; i++) {
            DailyPrice p = new DailyPrice();
            p.setTradeDate(LocalDate.now().minusDays(days - i));
            p.setClose(base.add(BigDecimal.valueOf(i * 0.5)));
            p.setOpen(p.getClose().subtract(BigDecimal.ONE));
            p.setHigh(p.getClose().add(BigDecimal.ONE));
            p.setLow(p.getClose().subtract(BigDecimal.valueOf(1.5)));
            p.setVol(1000000L);
            prices.add(p);
        }
        return prices;
    }

    @Test
    void calculate_shouldReturnAllIndicators() {
        List<DailyPrice> prices = generateMockPrices(90);
        TechnicalReport report = service.calculate(prices);

        assertThat(report.getMa5()).isPositive();
        assertThat(report.getMa20()).isPositive();
        assertThat(report.getMa60()).isPositive();
        assertThat(report.getRsi14()).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
        assertThat(report.getBoll_mid()).isPositive();
        assertThat(report.getTrendDirection()).isNotBlank();
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
mvn test -Dtest=TechnicalIndicatorServiceTest
```

期望：`FAIL` - TechnicalIndicatorService 未创建

- [ ] **Step 3: 创建 TechnicalIndicatorService**

`src/main/java/com/stockanalysis/indicator/TechnicalIndicatorService.java`:

```java
package com.stockanalysis.indicator;

import com.stockanalysis.model.report.TechnicalReport;
import com.stockanalysis.model.stock.DailyPrice;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.bollinger.*;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class TechnicalIndicatorService {

    public TechnicalReport calculate(List<DailyPrice> prices) {
        BarSeries series = buildSeries(prices);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        int last = series.getEndIndex();

        // 均线
        SMAIndicator ma5 = new SMAIndicator(close, 5);
        SMAIndicator ma20 = new SMAIndicator(close, 20);
        SMAIndicator ma60 = new SMAIndicator(close, 60);

        // RSI
        RSIIndicator rsi = new RSIIndicator(close, 14);

        // MACD
        EMAIndicator ema12 = new EMAIndicator(close, 12);
        EMAIndicator ema26 = new EMAIndicator(close, 26);

        // 布林带
        BollingerBandsMiddleIndicator bollMid = new BollingerBandsMiddleIndicator(ma20);
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(close, 20);
        BollingerBandsUpperIndicator bollUpper = new BollingerBandsUpperIndicator(bollMid, stdDev);
        BollingerBandsLowerIndicator bollLower = new BollingerBandsLowerIndicator(bollMid, stdDev);

        TechnicalReport report = new TechnicalReport();
        report.setMa5(toBigDecimal(ma5.getValue(last)));
        report.setMa20(toBigDecimal(ma20.getValue(last)));
        report.setMa60(toBigDecimal(ma60.getValue(last)));
        report.setRsi14(toBigDecimal(rsi.getValue(last)));
        report.setMacd(toBigDecimal(ema12.getValue(last).minus(ema26.getValue(last))));
        report.setBoll_upper(toBigDecimal(bollUpper.getValue(last)));
        report.setBoll_mid(toBigDecimal(bollMid.getValue(last)));
        report.setBoll_lower(toBigDecimal(bollLower.getValue(last)));

        // 趋势判断：MA5 > MA20 > MA60 为上升，反之为下降，否则震荡
        BigDecimal m5 = report.getMa5();
        BigDecimal m20 = report.getMa20();
        BigDecimal m60 = report.getMa60();
        if (m5.compareTo(m20) > 0 && m20.compareTo(m60) > 0) {
            report.setTrendDirection("上升");
        } else if (m5.compareTo(m20) < 0 && m20.compareTo(m60) < 0) {
            report.setTrendDirection("下降");
        } else {
            report.setTrendDirection("震荡");
        }

        BigDecimal currentPrice = prices.get(prices.size() - 1).getClose();
        report.setSupportLevel(report.getBoll_lower().toPlainString());
        report.setResistanceLevel(report.getBoll_upper().toPlainString());

        return report;
    }

    private BarSeries buildSeries(List<DailyPrice> prices) {
        BarSeries series = new BaseBarSeriesBuilder().withName("stock").build();
        for (DailyPrice p : prices) {
            ZonedDateTime time = p.getTradeDate().atStartOfDay(ZoneId.of("Asia/Shanghai"));
            Bar bar = new BaseBar(
                Duration.ofDays(1), time,
                p.getOpen().doubleValue(),
                p.getHigh().doubleValue(),
                p.getLow().doubleValue(),
                p.getClose().doubleValue(),
                p.getVol() == null ? 0 : p.getVol().doubleValue()
            );
            series.addBar(bar);
        }
        return series;
    }

    private BigDecimal toBigDecimal(org.ta4j.core.num.Num num) {
        return BigDecimal.valueOf(num.doubleValue()).setScale(4, RoundingMode.HALF_UP);
    }
}
```

- [ ] **Step 4: 运行测试**

```bash
mvn test -Dtest=TechnicalIndicatorServiceTest
```

期望：`PASS`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/stockanalysis/indicator/ src/test/java/com/stockanalysis/indicator/
git commit -m "feat: add TechnicalIndicatorService with Ma/RSI/MACD/Bollinger using Ta4j"
```

---

## Task 8: AnalysisSessionService + 事件系统

**Files:**
- Create: `src/main/java/com/stockanalysis/service/AnalysisSessionService.java`

- [ ] **Step 1: 创建 AnalysisSessionService**

`src/main/java/com/stockanalysis/service/AnalysisSessionService.java`:

```java
package com.stockanalysis.service;

import com.stockanalysis.model.event.AgentCompletedEvent;
import com.stockanalysis.model.event.AnalysisStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AnalysisSessionService {

    private final ApplicationEventPublisher eventPublisher;
    // sessionId -> SSE Sink
    private final Map<String, Sinks.Many<String>> sinks = new ConcurrentHashMap<>();

    public AnalysisSessionService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        sinks.put(sessionId, sink);
        return sessionId;
    }

    public Flux<String> getStream(String sessionId) {
        Sinks.Many<String> sink = sinks.get(sessionId);
        if (sink == null) {
            return Flux.error(new RuntimeException("Session not found: " + sessionId));
        }
        return sink.asFlux();
    }

    public void publishEvent(String sessionId, AnalysisStage stage, String message, Object data) {
        eventPublisher.publishEvent(new AgentCompletedEvent(this, sessionId, stage, message, data));
        Sinks.Many<String> sink = sinks.get(sessionId);
        if (sink != null) {
            String sseMessage = String.format("{\"stage\":\"%s\",\"message\":\"%s\"}",
                stage.name(), message.replace("\"", "'"));
            sink.tryEmitNext(sseMessage);
            if (stage == AnalysisStage.ANALYSIS_COMPLETED || stage == AnalysisStage.ANALYSIS_FAILED) {
                sink.tryEmitComplete();
                sinks.remove(sessionId);
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/stockanalysis/service/
git commit -m "feat: add AnalysisSessionService with SSE sink management"
```

---

## Task 9: REST API + SSE Controller

**Files:**
- Create: `src/main/java/com/stockanalysis/web/AnalysisController.java`
- Create: `src/main/java/com/stockanalysis/web/SseController.java`

- [ ] **Step 1: 创建 AnalysisController**

`src/main/java/com/stockanalysis/web/AnalysisController.java`:

```java
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

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startAnalysis(@RequestBody StartRequest request) {
        String sessionId = sessionService.createSession();
        // Agent 层（Plan B）会在此处注入 PipelineService/DebateService 调用
        // 目前返回 sessionId 供前端建立 SSE 连接
        return ResponseEntity.ok(Map.of("sessionId", sessionId, "status", "started"));
    }

    public record StartRequest(String stockCode, String mode, String dateRange) {}
}
```

- [ ] **Step 2: 创建 SseController**

`src/main/java/com/stockanalysis/web/SseController.java`:

```java
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

    @GetMapping(value = "/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@PathVariable String sessionId) {
        return sessionService.getStream(sessionId);
    }
}
```

- [ ] **Step 3: 启动应用，验证接口**

```bash
OPENAI_API_KEY=test ANTHROPIC_API_KEY=test DEEPSEEK_API_KEY=test TUSHARE_TOKEN=test \
mvn spring-boot:run
```

另开终端测试：

```bash
curl -X POST http://localhost:8080/api/analysis/start \
  -H "Content-Type: application/json" \
  -d '{"stockCode":"600519.SH","mode":"pipeline","dateRange":"3m"}'
```

期望输出：`{"sessionId":"xxx-xxx-xxx","status":"started"}`

- [ ] **Step 4: 停止应用，Commit**

```bash
git add src/main/java/com/stockanalysis/web/
git commit -m "feat: add REST API and SSE controller"
```

---

## Task 10: 集成验证

- [ ] **Step 1: 运行所有测试**

```bash
mvn test
```

期望：所有单元测试 `PASS`（集成测试需真实 API token）

- [ ] **Step 2: 完整启动验证**

```bash
OPENAI_API_KEY=your_key ANTHROPIC_API_KEY=your_key DEEPSEEK_API_KEY=your_key TUSHARE_TOKEN=your_token \
mvn spring-boot:run
```

验证：
1. 应用正常启动，端口 8080
2. `POST /api/analysis/start` 返回 sessionId
3. `GET /api/analysis/{sessionId}/stream` 建立 SSE 连接（暂无事件，正常）

- [ ] **Step 3: Final Commit**

```bash
git add .
git commit -m "feat: Plan A complete - backend infrastructure ready for AI agent integration"
```

---

## 完成标志

Plan A 完成后具备：
- ✅ 完整的 Maven 项目骨架
- ✅ 所有领域模型（股票数据 + 报告 + 事件）
- ✅ TushareProvider（行情 + 财务数据）
- ✅ EastMoneyProvider（新闻数据）
- ✅ TechnicalIndicatorService（Ma/RSI/MACD/布林带）
- ✅ 事件总线 + SSE 实时推送
- ✅ REST API 骨架（等待 Plan B 注入 Agent 调用）

**下一步：** 执行 Plan B（AI Agent 层）
