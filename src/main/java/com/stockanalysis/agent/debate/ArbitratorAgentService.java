package com.stockanalysis.agent.debate;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ArbitratorAgentService {

    @SystemMessage("""
            你是一位资深仲裁官。你的职责是综合评估多方、空方、中立三方的辩论论点，识别各方逻辑的强弱，给出公正的最终裁决。

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
            """)
    String adjudicate(@UserMessage String debateContext);
}
