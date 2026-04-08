package com.stockanalysis.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockanalysis.model.stock.DailyPrice;
import com.stockanalysis.model.stock.FinancialData;
import com.stockanalysis.model.stock.NewsItem;
import com.stockanalysis.model.stock.StockInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TushareProvider implements StockDataProvider {

    private final WebClient webClient;

    @Value("${app.api-keys.tushare}")
    private String token;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public TushareProvider(@Qualifier("tushareWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    private JsonNode callApi(String apiName, Map<String, Object> params, String fields) {
        Map<String, Object> body = Map.of(
            "api_name", apiName,
            "token", token,
            "params", params,
            "fields", fields
        );
        try {
            String response = webClient.post()
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            JsonNode root = objectMapper.readTree(response);
            if (root.path("code").asInt() != 0) {
                String msg = root.path("msg").asText();
                log.error("Tushare API error for {}: {}", apiName, msg);
                throw new RuntimeException("Tushare API error: " + msg);
            }
            return root.path("data");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Tushare API: " + apiName, e);
        }
    }

    @Override
    @Cacheable(value = "stockInfo", key = "#code")
    public StockInfo getStockInfo(String code) {
        JsonNode data = callApi("stock_basic",
            Map.of("ts_code", code, "list_status", "L"),
            "ts_code,name,industry,market,list_date");
        JsonNode items = data.path("items");
        if (!items.isArray() || items.isEmpty()) {
            throw new RuntimeException("Stock not found: " + code);
        }
        JsonNode item = items.get(0);
        StockInfo info = new StockInfo();
        info.setCode(item.get(0).asText());
        info.setName(item.get(1).asText());
        info.setIndustry(item.get(2).asText());
        info.setMarket(item.get(3).asText());
        String listDateStr = item.get(4).asText();
        info.setListDate(listDateStr.isEmpty() ? null : LocalDate.parse(listDateStr, DATE_FMT));
        return info;
    }

    @Override
    @Cacheable(value = "dailyPrices", key = "#code + '_' + #startDate + '_' + #endDate")
    public List<DailyPrice> getDailyPrices(String code, LocalDate startDate, LocalDate endDate) {
        JsonNode data = callApi("daily",
            Map.of("ts_code", code,
                   "start_date", startDate.format(DATE_FMT),
                   "end_date", endDate.format(DATE_FMT)),
            "trade_date,open,high,low,close,pre_close,change,pct_chg,vol,amount");
        JsonNode items = data.path("items");
        List<DailyPrice> prices = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode item : items) {
                DailyPrice price = new DailyPrice();
                price.setTradeDate(LocalDate.parse(item.get(0).asText(), DATE_FMT));
                price.setOpen(parseBigDecimal(item.get(1)));
                price.setHigh(parseBigDecimal(item.get(2)));
                price.setLow(parseBigDecimal(item.get(3)));
                price.setClose(parseBigDecimal(item.get(4)));
                price.setPreClose(parseBigDecimal(item.get(5)));
                price.setChange(parseBigDecimal(item.get(6)));
                price.setPctChg(parseBigDecimal(item.get(7)));
                price.setVol(item.get(8).isNull() ? 0L : item.get(8).asLong());
                price.setAmount(parseBigDecimal(item.get(9)));
                prices.add(price);
            }
        }
        // Tushare 返回倒序，转为升序
        prices.sort((a, b) -> a.getTradeDate().compareTo(b.getTradeDate()));
        return prices;
    }

    @Override
    @Cacheable(value = "financialData", key = "#code")
    public FinancialData getFinancials(String code) {
        FinancialData fd = new FinancialData();
        fd.setCode(code);
        try {
            JsonNode finaData = callApi("fina_indicator",
                Map.of("ts_code", code, "limit", 1),
                "ts_code,end_date,roe,debt_to_assets");
            JsonNode finaItems = finaData.path("items");
            if (finaItems.isArray() && !finaItems.isEmpty()) {
                JsonNode item = finaItems.get(0);
                fd.setReportPeriod(item.get(1).asText());
                fd.setRoe(parseBigDecimal(item.get(2)));
                fd.setDebtRatio(parseBigDecimal(item.get(3)));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch fina_indicator for {}: {}", code, e.getMessage());
        }
        try {
            String today = LocalDate.now().format(DATE_FMT);
            JsonNode valData = callApi("daily_basic",
                Map.of("ts_code", code, "trade_date", today),
                "ts_code,pe,pb,total_mv");
            JsonNode valItems = valData.path("items");
            if (valItems.isArray() && !valItems.isEmpty()) {
                JsonNode item = valItems.get(0);
                fd.setPe(parseBigDecimal(item.get(1)));
                fd.setPb(parseBigDecimal(item.get(2)));
                // total_mv 单位为万元，转为亿元
                BigDecimal mv = parseBigDecimal(item.get(3));
                fd.setTotalMarketCap(mv == null ? null : mv.divide(BigDecimal.valueOf(10000)));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch daily_basic for {}: {}", code, e.getMessage());
        }
        return fd;
    }

    @Override
    public List<NewsItem> getNews(String code, int limit) {
        // Tushare 新闻接口积分要求较高，由 EastMoneyProvider 提供
        return new ArrayList<>();
    }

    private BigDecimal parseBigDecimal(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String text = node.asText();
        if (text.isEmpty() || "null".equals(text)) return null;
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
