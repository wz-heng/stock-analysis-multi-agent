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
