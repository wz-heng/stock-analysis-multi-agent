package com.stockanalysis.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean("tushareWebClient")
    public WebClient tushareWebClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(30));
        return WebClient.builder()
            .baseUrl("https://api.tushare.pro")
            .defaultHeader("Content-Type", "application/json")
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }

    @Bean("eastMoneyWebClient")
    // 注意：此 Bean 仅适用于公告接口（np-anotice-stock），其他东方财富接口需单独 Bean
    public WebClient eastMoneyWebClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(15));
        return WebClient.builder()
            .baseUrl("https://np-anotice-stock.eastmoney.com")
            .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; StockAnalysis/1.0)")
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }
}
