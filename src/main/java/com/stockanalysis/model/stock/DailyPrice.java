package com.stockanalysis.model.stock;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailyPrice {
    private LocalDate tradeDate;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal preClose;
    private BigDecimal change;
    private BigDecimal pctChg;   // 涨跌幅(%)
    private Long vol;            // 成交量(手)
    private BigDecimal amount;   // 成交额(千元)
}
