package com.stockanalysis.data;

import com.stockanalysis.model.stock.DailyPrice;
import com.stockanalysis.model.stock.FinancialData;
import com.stockanalysis.model.stock.NewsItem;
import com.stockanalysis.model.stock.StockInfo;

import java.time.LocalDate;
import java.util.List;

public interface StockDataProvider {

    /**
     * 获取股票基本信息
     * @param code 股票代码，如 "600519.SH"
     */
    StockInfo getStockInfo(String code);

    /**
     * 获取日线行情数据（按日期升序返回）
     * @param code 股票代码
     * @param startDate 开始日期（含）
     * @param endDate 结束日期（含）
     */
    List<DailyPrice> getDailyPrices(String code, LocalDate startDate, LocalDate endDate);

    /**
     * 获取最新财务数据
     * @param code 股票代码
     */
    FinancialData getFinancials(String code);

    /**
     * 获取最新新闻列表
     * @param code 股票代码
     * @param limit 最多返回条数
     */
    List<NewsItem> getNews(String code, int limit);
}
