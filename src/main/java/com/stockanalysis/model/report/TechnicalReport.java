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
    private String trendDirection;    // 上升/下降/震荡
    private String supportLevel;
    private String resistanceLevel;
    private String analysisText;
    private String technicalRating;   // 强势/中性/弱势
}
