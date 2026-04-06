package com.stockanalysis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean("tushareWebClient")
    public WebClient tushareWebClient() {
        return WebClient.builder()
            .baseUrl("https://api.tushare.pro")
            .defaultHeader("Content-Type", "application/json")
            .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }

    @Bean("eastMoneyWebClient")
    public WebClient eastMoneyWebClient() {
        return WebClient.builder()
            .baseUrl("https://np-anotice-stock.eastmoney.com")
            .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; StockAnalysis/1.0)")
            .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }
}
