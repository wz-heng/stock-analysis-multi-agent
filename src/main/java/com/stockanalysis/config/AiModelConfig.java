package com.stockanalysis.config;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AiModelConfig {

    // GPT-4o 由 langchain4j-open-ai-spring-boot-starter 通过 application.yml 自动配置
    // bean 名称为 "openAiChatModel"，是 ChatLanguageModel 的 @Primary bean

    @Bean("anthropicChatModel")
    public ChatLanguageModel anthropicChatModel(
            @Value("${app.api-keys.anthropic}") String apiKey,
            @Value("${app.models.claude.model-name}") String modelName,
            @Value("${app.models.claude.max-tokens}") int maxTokens,
            @Value("${app.models.claude.temperature}") double temperature) {
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();
    }

    @Bean("deepSeekChatModel")
    public ChatLanguageModel deepSeekChatModel(
            @Value("${app.api-keys.deepseek}") String apiKey,
            @Value("${app.models.deepseek.base-url}") String baseUrl,
            @Value("${app.models.deepseek.model-name}") String modelName,
            @Value("${app.models.deepseek.temperature}") double temperature,
            @Value("${app.models.deepseek.timeout-seconds}") int timeoutSeconds) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }
}
