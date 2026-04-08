package com.stockanalysis.model.report;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvestmentDecision {
    private String rating;              // 强烈买入/买入/持有/卖出/强烈卖出
    private BigDecimal targetPriceLow;
    private BigDecimal targetPriceHigh;
    private int confidencePercent;      // 0-100
    private String coreLogic;
    private String mainRisks;
    private String summaryText;
}
