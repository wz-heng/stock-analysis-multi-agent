package com.stockanalysis.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockanalysis.model.stock.DailyPrice;
import com.stockanalysis.model.stock.FinancialData;
import com.stockanalysis.model.stock.NewsItem;
import com.stockanalysis.model.stock.StockInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class EastMoneyProvider implements StockDataProvider {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EastMoneyProvider(@Qualifier("eastMoneyWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * 将 Tushare 格式股票代码转换为东方财富格式
     * SH -> 0, SZ -> 1
     * 例: 600519.SH -> 0.600519
     */
    private String convertCode(String code) {
        String[] parts = code.split("\\.");
        if (parts.length != 2) return code;
        String market = "SH".equals(parts[1]) ? "0" : "1";
        return market + "." + parts[0];
    }

    @Override
    @Cacheable(value = "news", key = "#code + '_' + #limit")
    public List<NewsItem> getNews(String code, int limit) {
        String emCode = convertCode(code);
        try {
            String url = "/api/security/ann?sr=-1&page=1&ps=" + limit + "&id=" + emCode + "&rt=12";
            String response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            JsonNode root = objectMapper.readTree(response);
            JsonNode list = root.path("data").path("list");
            List<NewsItem> newsList = new ArrayList<>();
            if (list.isArray()) {
                for (JsonNode item : list) {
                    NewsItem news = new NewsItem();
                    news.setTitle(item.path("NOTICE_TITLE").asText(""));
                    news.setContent(item.path("NOTICE_CONTENT").asText(""));
                    news.setSource("东方财富");
                    String timeStr = item.path("NOTICE_DATE").asText("");
                    news.setPublishTime(parseDateTime(timeStr));
                    news.setUrl("https://www.eastmoney.com");
                    newsList.add(news);
                }
            }
            log.info("Fetched {} news items for {}", newsList.size(), code);
            return newsList;
        } catch (Exception e) {
            log.warn("EastMoney news fetch failed for {}: {}", code, e.getMessage());
            return new ArrayList<>();
        }
    }

    private LocalDateTime parseDateTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e1) {
            try {
                return LocalDate.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    .atStartOfDay();
            } catch (DateTimeParseException e2) {
                return LocalDateTime.now();
            }
        }
    }

    // EastMoneyProvider 仅提供新闻数据
    @Override
    public StockInfo getStockInfo(String code) {
        throw new UnsupportedOperationException("Use TushareProvider for stock info");
    }

    @Override
    public List<DailyPrice> getDailyPrices(String code, LocalDate startDate, LocalDate endDate) {
        throw new UnsupportedOperationException("Use TushareProvider for daily prices");
    }

    @Override
    public FinancialData getFinancials(String code) {
        throw new UnsupportedOperationException("Use TushareProvider for financial data");
    }
}
