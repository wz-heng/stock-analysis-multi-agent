package com.stockanalysis.model.stock;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NewsItem {
    private String title;
    private String content;
    private String source;
    private LocalDateTime publishTime;
    private String url;
}
