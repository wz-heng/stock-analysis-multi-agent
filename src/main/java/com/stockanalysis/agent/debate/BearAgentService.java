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
