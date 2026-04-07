package com.stockanalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmResponseParserTest {

    private final LlmResponseParser parser = new LlmResponseParser();

    @Test
    void extractJson_plainJson() {
        String response = "{\"rating\":\"买入\",\"confidence\":75}";
        JsonNode node = parser.extractJson(response);
        assertThat(node.get("rating").asText()).isEqualTo("买入");
        assertThat(node.get("confidence").asInt()).isEqualTo(75);
    }

    @Test
    void extractJson_jsonInMarkdownBlock() {
        String response = "分析结果如下：\n```json\n{\"rating\":\"持有\"}\n```";
        JsonNode node = parser.extractJson(response);
        assertThat(node.get("rating").asText()).isEqualTo("持有");
    }

    @Test
    void extractJson_jsonEmbeddedInText() {
        String response = "根据分析，{\"rating\":\"卖出\",\"score\":30} 这是我的结论。";
        JsonNode node = parser.extractJson(response);
        assertThat(node.get("rating").asText()).isEqualTo("卖出");
    }

    @Test
    void getString_returnsFieldValue() {
        String response = "{\"analysisText\":\"股票表现良好\"}";
        String text = parser.getString(response, "analysisText", "默认值");
        assertThat(text).isEqualTo("股票表现良好");
    }

    @Test
    void getString_returnsDefaultOnMissingField() {
        String response = "{\"other\":\"value\"}";
        String text = parser.getString(response, "analysisText", "默认值");
        assertThat(text).isEqualTo("默认值");
    }

    @Test
    void getDouble_returnsNumericValue() {
        String response = "{\"sentimentScore\":0.75}";
        double score = parser.getDouble(response, "sentimentScore", 0.0);
        assertThat(score).isEqualTo(0.75);
    }

    @Test
    void getInt_returnsIntValue() {
        String response = "{\"confidencePercent\":80}";
        int confidence = parser.getInt(response, "confidencePercent", 50);
        assertThat(confidence).isEqualTo(80);
    }
}
