package com.stockanalysis.data;

import com.stockanalysis.model.stock.DailyPrice;
import com.stockanalysis.model.stock.StockInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 集成测试：需要真实 Tushare token。运行方式：
// TUSHARE_TOKEN=your_token mvn test -Dtest=TushareProviderTest
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class TushareProviderTest {

    @Autowired
    private TushareProvider tushareProvider;

    @Test
    void getStockInfo_shouldReturnValidInfo() {
        StockInfo info = tushareProvider.getStockInfo("600519.SH");
        assertThat(info).isNotNull();
        assertThat(info.getCode()).isEqualTo("600519.SH");
        assertThat(info.getName()).isNotBlank();
    }

    @Test
    void getDailyPrices_shouldReturnNonEmptyList() {
        List<DailyPrice> prices = tushareProvider.getDailyPrices(
            "600519.SH",
            LocalDate.now().minusDays(30),
            LocalDate.now()
        );
        assertThat(prices).isNotEmpty();
        assertThat(prices.get(0).getClose()).isPositive();
    }
}
