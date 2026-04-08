package com.stockanalysis.indicator;

import com.stockanalysis.model.report.TechnicalReport;
import com.stockanalysis.model.stock.DailyPrice;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.bollinger.*;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class TechnicalIndicatorService {

    public TechnicalReport calculate(List<DailyPrice> prices) {
        BarSeries series = buildSeries(prices);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        int last = series.getEndIndex();

        SMAIndicator ma5  = new SMAIndicator(close, 5);
        SMAIndicator ma20 = new SMAIndicator(close, 20);
        SMAIndicator ma60 = new SMAIndicator(close, 60);
        RSIIndicator  rsi  = new RSIIndicator(close, 14);
        EMAIndicator  ema12 = new EMAIndicator(close, 12);
        EMAIndicator  ema26 = new EMAIndicator(close, 26);

        BollingerBandsMiddleIndicator bollMid   = new BollingerBandsMiddleIndicator(ma20);
        StandardDeviationIndicator    stdDev    = new StandardDeviationIndicator(close, 20);
        BollingerBandsUpperIndicator  bollUpper = new BollingerBandsUpperIndicator(bollMid, stdDev);
        BollingerBandsLowerIndicator  bollLower = new BollingerBandsLowerIndicator(bollMid, stdDev);

        TechnicalReport report = new TechnicalReport();
        report.setMa5(dec(ma5.getValue(last)));
        report.setMa20(dec(ma20.getValue(last)));
        report.setMa60(dec(ma60.getValue(last)));
        report.setRsi14(dec(rsi.getValue(last)));
        report.setMacd(dec(ema12.getValue(last).minus(ema26.getValue(last))));
        report.setBoll_upper(dec(bollUpper.getValue(last)));
        report.setBoll_mid(dec(bollMid.getValue(last)));
        report.setBoll_lower(dec(bollLower.getValue(last)));

        BigDecimal m5  = report.getMa5();
        BigDecimal m20 = report.getMa20();
        BigDecimal m60 = report.getMa60();
        if (m5.compareTo(m20) > 0 && m20.compareTo(m60) > 0) {
            report.setTrendDirection("上升");
        } else if (m5.compareTo(m20) < 0 && m20.compareTo(m60) < 0) {
            report.setTrendDirection("下降");
        } else {
            report.setTrendDirection("震荡");
        }

        report.setSupportLevel(report.getBoll_lower().toPlainString());
        report.setResistanceLevel(report.getBoll_upper().toPlainString());
        return report;
    }

    private BarSeries buildSeries(List<DailyPrice> prices) {
        BarSeries series = new BaseBarSeriesBuilder().withName("stock").build();
        for (DailyPrice p : prices) {
            ZonedDateTime time = p.getTradeDate().atStartOfDay(ZoneId.of("Asia/Shanghai"));
            series.addBar(Duration.ofDays(1), time,
                    p.getOpen().doubleValue(),
                    p.getHigh().doubleValue(),
                    p.getLow().doubleValue(),
                    p.getClose().doubleValue(),
                    p.getVol() == null ? 0.0 : p.getVol().doubleValue());
        }
        return series;
    }

    private BigDecimal dec(org.ta4j.core.num.Num num) {
        return BigDecimal.valueOf(num.doubleValue()).setScale(4, RoundingMode.HALF_UP);
    }
}
