package com.stockanalysis.model.stock;

import lombok.Data;
import java.time.LocalDate;

@Data
public class StockInfo {
    private String code;        // 股票代码，如 600519.SH
    private String name;        // 股票名称
    private String industry;    // 所属行业
    private String market;      // 市场（上交所/深交所）
    private LocalDate listDate; // 上市日期
}
