package com.stockanalysis.model.report;
import lombok.Data;
import java.util.List;

@Data
public class SentimentReport {
    private double sentimentScore;     // -1.0 ~ 1.0
    private String sentimentLabel;     // 极度乐观/乐观/中性/悲观/极度悲观
    private List<String> keyNewsPoints;
    private String analysisText;
}
