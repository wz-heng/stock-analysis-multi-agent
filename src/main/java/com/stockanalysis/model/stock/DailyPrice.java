package com.stockanalysis.model.stock;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }

    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }

    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }

    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }

    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal close) { this.close = close; }

    public BigDecimal getPreClose() { return preClose; }
    public void setPreClose(BigDecimal preClose) { this.preClose = preClose; }

    public BigDecimal getChange() { return change; }
    public void setChange(BigDecimal change) { this.change = change; }

    public BigDecimal getPctChg() { return pctChg; }
    public void setPctChg(BigDecimal pctChg) { this.pctChg = pctChg; }

    public Long getVol() { return vol; }
    public void setVol(Long vol) { this.vol = vol; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
