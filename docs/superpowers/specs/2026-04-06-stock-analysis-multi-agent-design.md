# 多Agent股票分析系统设计文档

**日期：** 2026-04-06  
**项目：** stock-analysis-multi-agent  
**状态：** 已审批

---

## 一、项目概述

基于 Java 的多 Agent 股票分析系统，针对 A 股市场，提供两种核心分析模式：

1. **流水线模式**：研究员 → 技术分析师 → 舆情分析师 → 投资经理，顺序分析，层层递进
2. **辩论模式**：多方 / 空方 / 中立三个 LLM 并行对抗，仲裁官综合裁决
3. **串联模式**：流水线报告作为辩论背景，两阶段完整分析

---

## 二、技术栈

| 层次 | 技术选型 |
|------|---------|
| 后端框架 | Spring Boot 3.x |
| Agent 框架 | LangChain4j |
| 技术指标 | Ta4j |
| 本地缓存 | Caffeine + Spring Cache |
| 前端框架 | Vue 3 + Vite + Pinia |
| UI 组件库 | Element Plus |
| 图表库 | ECharts |
| 实时通信 | SSE（Server-Sent Events） |

---

## 三、LLM 分配

| Agent | 模型 | 理由 |
|-------|------|------|
| 研究员 | GPT-4o | 综合分析能力强 |
| 技术分析师 | GPT-4o | 数值推理精准 |
| 舆情分析师 | DeepSeek | 中文语境理解强，A股新闻处理更精准 |
| 投资经理 | GPT-4o | 综合决策能力强 |
| 辩论多方 | GPT-4o | 擅长找利好逻辑 |
| 辩论空方 | Claude | 逻辑严谨，批判性强 |
| 辩论中立 | DeepSeek | 中文语境优势，客观评估 |
| 仲裁官 | GPT-4o | 综合推理，最终裁决 |

---

## 四、整体架构

```
┌─────────────────────────────────────────────────────┐
│                  Spring Boot 应用                    │
│                                                     │
│  ┌──────────┐    ┌─────────────────────────────┐   │
│  │  Web层   │    │         事件总线              │   │
│  │ REST API │    │  (Spring ApplicationEvent)   │   │
│  │   SSE   │◄───│                             │   │
│  └──────────┘    └─────────────────────────────┘   │
│                          ▲  │                       │
│                          │  ▼                       │
│  ┌───────────────────────────────────────────────┐  │
│  │              Agent 层（LangChain4j）           │  │
│  │                                               │  │
│  │  [流水线]  研究员→技术分析师→舆情分析师→投资经理│  │
│  │  [辩论]   多方 ║ 空方 ║ 中立  →  仲裁官       │  │
│  └───────────────────────────────────────────────┘  │
│                          │                          │
│                          ▼                          │
│  ┌───────────────────────────────────────────────┐  │
│  │              数据层（可插拔接口）               │  │
│  │   Tushare API │ 东方财富新闻 │ Ta4j指标计算    │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘

前端（Vue 3 + Vite）        后端（Spring Boot）
localhost:5173         ←→   localhost:8080
```

---

## 五、流水线 Agent 设计

### 数据流

```
输入：股票代码 + 分析时间范围
    │
    ▼
ResearcherAgent（GPT-4o）
  职责：基本面数据收集
  工具：getStockInfo, getFinancials
  输出：BasicResearchReport
    │
    ▼
TechnicalAnalystAgent（GPT-4o）
  职责：技术指标分析（MA/MACD/RSI/KDJ/布林带）
  工具：getDailyPrices, getTechnicalIndicators
  输出：TechnicalReport
    │
    ▼
SentimentAnalystAgent（DeepSeek）
  职责：新闻舆情分析，市场情绪评估
  工具：getNews, getMoneyFlow
  输出：SentimentReport
    │
    ▼
InvestmentManagerAgent（GPT-4o）
  职责：综合三份报告，给出投资建议
  输入：BasicResearchReport + TechnicalReport + SentimentReport
  输出：InvestmentDecision（评级/目标价/核心逻辑/主要风险）
```

### 关键原则

- 每个 Agent 完成后发布 `AgentCompletedEvent`，前端实时收到进度
- InvestmentManagerAgent 接收完整的前三份报告作为 context
- 技术指标由 Ta4j 本地计算，结果以结构化数据传入 LLM，不让 LLM 自己计算数值

---

## 六、辩论引擎设计

### 数据流

```
输入：股票代码 + 背景报告（可选）
    │
    ├──────────────┬──────────────┐
    ▼              ▼              ▼
BullAgent      BearAgent     NeutralAgent
（GPT-4o）    （Claude）     （DeepSeek）
 多方论点      空方论点       中立评估
    │              │              │
    └──────────────┴──────────────┘
                   │ 三方论点汇总
                   ▼
           ArbitratorAgent（GPT-4o）
           综合裁决：评级 + 置信度 + 核心依据
           可选：触发第二轮针对分歧点的辩论
```

### 关键原则

- 三方 Agent **完全并行**调用，互相不可见对方论点，保证独立性
- 仲裁官拿到三份完整论点后才开始推理
- 支持多轮辩论：仲裁官识别重大分歧时，可针对争议点触发第二轮

---

## 七、数据层设计

### 接口抽象

```java
interface StockDataProvider {
    StockInfo getStockInfo(String code);
    List<DailyPrice> getDailyPrices(String code, LocalDate start, LocalDate end);
    FinancialData getFinancials(String code);
    List<NewsItem> getNews(String code, int limit);
}
```

### 实现

| 实现类 | 数据源 | 提供数据 |
|--------|--------|---------|
| TushareProvider | Tushare Pro | 日线行情、基础财务指标 |
| EastMoneyProvider | 东方财富非官方API | 新闻列表、公告、评论 |

### 缓存策略

| 数据类型 | 缓存时长 |
|---------|---------|
| 日线数据 | 当天 |
| 财务数据 | 1周 |
| 新闻数据 | 1小时 |

缓存实现：Spring Cache + Caffeine（纯内存，无需 Redis）

### 可插拔设计

所有 Agent 只依赖 `StockDataProvider` 接口，未来切换付费数据源只需新增实现类，Agent 代码零改动。

---

## 八、前端设计

### 页面结构

**页面一：分析入口**
- 股票代码输入
- 分析模式选择（流水线 / 辩论 / 串联）
- 时间范围选择

**页面二：实时分析过程**
- 流水线进度卡片（SSE实时更新）
- 辩论进度卡片（SSE实时更新）
- 实时日志滚动区域
- ECharts K线图 + 技术指标图

**页面三：最终报告**
- 综合评级 + 置信度可视化
- 各 Agent 报告折叠展示
- 辩论三方论点对比
- 仲裁官裁决
- 导出 PDF

### 通信方式

- 触发分析：`POST /api/analysis/start`
- 实时进度：`GET /api/analysis/{id}/stream`（SSE）
- 获取报告：`GET /api/analysis/{id}/report`

---

## 九、项目结构（后端）

```
stock-analysis-multi-agent/
├── src/main/java/com/stockanalysis/
│   ├── agent/
│   │   ├── pipeline/
│   │   │   ├── ResearcherAgent.java
│   │   │   ├── TechnicalAnalystAgent.java
│   │   │   ├── SentimentAnalystAgent.java
│   │   │   └── InvestmentManagerAgent.java
│   │   └── debate/
│   │       ├── BullAgent.java
│   │       ├── BearAgent.java
│   │       ├── NeutralAgent.java
│   │       └── ArbitratorAgent.java
│   ├── data/
│   │   ├── StockDataProvider.java
│   │   ├── TushareProvider.java
│   │   └── EastMoneyProvider.java
│   ├── indicator/
│   │   └── TechnicalIndicatorService.java
│   ├── event/
│   │   └── AgentCompletedEvent.java
│   ├── service/
│   │   ├── PipelineService.java
│   │   └── DebateService.java
│   └── web/
│       ├── AnalysisController.java
│       └── SseController.java
├── src/main/resources/
│   └── application.yml
├── frontend/          ← Vue 3 + Vite
└── docs/
    └── superpowers/specs/
        └── 2026-04-06-stock-analysis-multi-agent-design.md
```

---

## 十、运行环境要求

- JDK 17+
- Node.js 18+（前端构建）
- 内存：≥ 2GB（4GB 推荐）
- 网络：稳定代理（访问 OpenAI / Claude API）
- API Key：OpenAI、Anthropic（Claude）、DeepSeek、Tushare Pro

---

## 十一、数据流示意（串联模式）

```
用户输入股票代码
    │
    ▼
[流水线阶段]
研究员 → 技术分析师 → 舆情分析师 → 投资经理
    │
    ▼ 流水线报告作为背景
[辩论阶段]
多方 ║ 空方 ║ 中立（并行，基于流水线报告）
    │
    ▼
仲裁官综合裁决
    │
    ▼
最终报告推送前端
```
