package com.stockanalysis.model.stock;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class FinancialData {
    private String code;
    private String reportPeriod;
    private BigDecimal pe;
    private BigDecimal pb;
    private BigDecimal roe;
    private BigDecimal revenueGrowth;
    private BigDecimal netProfitGrowth;
    private BigDecimal debtRatio;
    private BigDecimal totalMarketCap;  // 总市值(亿元)
}
