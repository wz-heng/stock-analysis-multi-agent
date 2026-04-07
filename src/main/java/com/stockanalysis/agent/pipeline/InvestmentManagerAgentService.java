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
            """)
    String decide(@UserMessage String allReports);
}
