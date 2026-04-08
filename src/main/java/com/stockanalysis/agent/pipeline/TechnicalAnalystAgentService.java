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
