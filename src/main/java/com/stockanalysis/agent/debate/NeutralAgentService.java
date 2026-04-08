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
