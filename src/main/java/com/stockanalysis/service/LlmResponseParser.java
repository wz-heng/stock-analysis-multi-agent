package com.stockanalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class LlmResponseParser {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```");
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*\\}");

    public JsonNode extractJson(String response) {
        if (response == null || response.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(response.trim());
        } catch (Exception ignored) {}

        Matcher blockMatcher = JSON_BLOCK.matcher(response);
        if (blockMatcher.find()) {
            try {
                return objectMapper.readTree(blockMatcher.group(1).trim());
            } catch (Exception ignored) {}
        }

        Matcher objMatcher = JSON_OBJECT.matcher(response);
        while (objMatcher.find()) {
            try {
                return objectMapper.readTree(objMatcher.group());
            } catch (Exception ignored) {}
        }

        log.warn("Could not extract JSON from LLM response, returning empty node.");
        return objectMapper.createObjectNode();
    }

    public String getString(String response, String field, String defaultValue) {
        JsonNode node = extractJson(response);
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return defaultValue;
        return fieldNode.asText(defaultValue);
    }

    public double getDouble(String response, String field, double defaultValue) {
        JsonNode node = extractJson(response);
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return defaultValue;
        return fieldNode.asDouble(defaultValue);
    }

    public int getInt(String response, String field, int defaultValue) {
        JsonNode node = extractJson(response);
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return defaultValue;
        return fieldNode.asInt(defaultValue);
    }

    public boolean getBoolean(String response, String field, boolean defaultValue) {
        JsonNode node = extractJson(response);
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return defaultValue;
        return fieldNode.asBoolean(defaultValue);
    }
}
