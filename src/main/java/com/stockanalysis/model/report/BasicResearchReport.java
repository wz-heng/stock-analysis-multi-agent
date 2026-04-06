package com.stockanalysis.model.report;
import com.stockanalysis.model.stock.FinancialData;
import com.stockanalysis.model.stock.StockInfo;
import lombok.Data;

@Data
public class BasicResearchReport {
    private StockInfo stockInfo;
    private FinancialData financialData;
    private String analysisText;
    private String fundamentalRating;  // 优秀/良好/一般/较差
}
