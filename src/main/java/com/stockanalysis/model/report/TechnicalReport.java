package com.stockanalysis.model.report;

import java.math.BigDecimal;

public class TechnicalReport {
    private BigDecimal ma5;
    private BigDecimal ma20;
    private BigDecimal ma60;
    private BigDecimal macd;
    private BigDecimal rsi14;
    private BigDecimal kdj_k;
    private BigDecimal kdj_d;
    private BigDecimal boll_upper;
    private BigDecimal boll_mid;
    private BigDecimal boll_lower;
    private String trendDirection;    // 上升/下降/震荡
    private String supportLevel;
    private String resistanceLevel;
    private String analysisText;
    private String technicalRating;   // 强势/中性/弱势

    public BigDecimal getMa5() { return ma5; }
    public void setMa5(BigDecimal ma5) { this.ma5 = ma5; }

    public BigDecimal getMa20() { return ma20; }
    public void setMa20(BigDecimal ma20) { this.ma20 = ma20; }

    public BigDecimal getMa60() { return ma60; }
    public void setMa60(BigDecimal ma60) { this.ma60 = ma60; }

    public BigDecimal getMacd() { return macd; }
    public void setMacd(BigDecimal macd) { this.macd = macd; }

    public BigDecimal getRsi14() { return rsi14; }
    public void setRsi14(BigDecimal rsi14) { this.rsi14 = rsi14; }

    public BigDecimal getKdj_k() { return kdj_k; }
    public void setKdj_k(BigDecimal kdj_k) { this.kdj_k = kdj_k; }

    public BigDecimal getKdj_d() { return kdj_d; }
    public void setKdj_d(BigDecimal kdj_d) { this.kdj_d = kdj_d; }

    public BigDecimal getBoll_upper() { return boll_upper; }
    public void setBoll_upper(BigDecimal boll_upper) { this.boll_upper = boll_upper; }

    public BigDecimal getBoll_mid() { return boll_mid; }
    public void setBoll_mid(BigDecimal boll_mid) { this.boll_mid = boll_mid; }

    public BigDecimal getBoll_lower() { return boll_lower; }
    public void setBoll_lower(BigDecimal boll_lower) { this.boll_lower = boll_lower; }

    public String getTrendDirection() { return trendDirection; }
    public void setTrendDirection(String trendDirection) { this.trendDirection = trendDirection; }

    public String getSupportLevel() { return supportLevel; }
    public void setSupportLevel(String supportLevel) { this.supportLevel = supportLevel; }

    public String getResistanceLevel() { return resistanceLevel; }
    public void setResistanceLevel(String resistanceLevel) { this.resistanceLevel = resistanceLevel; }

    public String getAnalysisText() { return analysisText; }
    public void setAnalysisText(String analysisText) { this.analysisText = analysisText; }

    public String getTechnicalRating() { return technicalRating; }
    public void setTechnicalRating(String technicalRating) { this.technicalRating = technicalRating; }
}
