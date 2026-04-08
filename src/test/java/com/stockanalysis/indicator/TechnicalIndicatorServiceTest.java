package com.stockanalysis.indicator;

import com.stockanalysis.model.report.TechnicalReport;
import com.stockanalysis.model.stock.DailyPrice;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TechnicalIndicatorServiceTest {

    private final TechnicalIndicatorService service = new TechnicalIndicatorService();

    private List<DailyPrice> generateMockPrices(int days) {
        List<DailyPrice> prices = new ArrayList<>();
        BigDecimal base = BigDecimal.valueOf(100);
        for (int i = 0; i < days; i++) {
            DailyPrice p = new DailyPrice();
            p.setTradeDate(LocalDate.now().minusDays(days - i));
            p.setClose(base.add(BigDecimal.valueOf(i * 0.5)));
            p.setOpen(p.getClose().subtract(BigDecimal.ONE));
            p.setHigh(p.getClose().add(BigDecimal.ONE));
            p.setLow(p.getClose().subtract(BigDecimal.valueOf(1.5)));
            p.setVol(1000000L);
            prices.add(p);
        }
        return prices;
    }

    @Test
    void calculate_shouldReturnAllIndicators() {
        List<DailyPrice> prices = generateMockPrices(90);
        TechnicalReport report = service.calculate(prices);

        assertThat(report.getMa5()).isPositive();
        assertThat(report.getMa20()).isPositive();
        assertThat(report.getMa60()).isPositive();
        assertThat(report.getRsi14()).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
        assertThat(report.getBoll_mid()).isPositive();
        assertThat(report.getTrendDirection()).isIn("上升", "下降", "震荡");
    }

    @Test
    void calculate_uptrend_shouldReturnUpDirection() {
        List<DailyPrice> prices = generateMockPrices(90); // 单调上涨
        TechnicalReport report = service.calculate(prices);
        assertThat(report.getTrendDirection()).isEqualTo("上升");
    }
}
