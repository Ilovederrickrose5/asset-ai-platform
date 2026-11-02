package com.enterprise.asset.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Configuration
public class OpenAiChatConfig {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiChatConfig.class);

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:qwen-turbo}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:2048}")
    private Integer maxTokens;

    @Bean
    public OpenAiApi openAiApi() {
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        logger.info("OpenAI compatible base url: {}", normalizedBaseUrl);
        return new OpenAiApi(normalizedBaseUrl, apiKey);
    }

    @Bean
    public OpenAiChatOptions chatOptions() {
        return OpenAiChatOptions.builder()
                .withModel(model)
                .withTemperature(temperature)
                .withMaxTokens(maxTokens)
                .withTopP(0.9)
                .withFrequencyPenalty(0.0)
                .withPresencePenalty(0.0)
                .build();
    }

    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi, OpenAiChatOptions chatOptions) {
        return new OpenAiChatModel(openAiApi, chatOptions);
    }

    @Bean
    @Primary
    public ChatClient.Builder chatClientBuilder(OpenAiChatModel chatModel) {
        // RAG 检索统一由 ChatService 手动执行（带 tenant 过滤），不挂 QuestionAnswerAdvisor
        logger.info("ChatClient initialized with SimpleLoggerAdvisor only; RAG retrieval delegated to ChatService");
        return ChatClient.builder(chatModel)
                .defaultOptions(chatOptions())
                .defaultAdvisors(new SimpleLoggerAdvisor());
    }

    @Bean
    public ApplicationRunner aiConnectionWarmup(ChatClient.Builder chatClientBuilder) {
        return args -> {
            if (apiKey == null || apiKey.isBlank()) {
                logger.info("Skip AI connection warmup: api key not configured");
                return;
            }

            // 预热不是启动依赖：异步执行，避免上游不可用时拖慢启动与 Nacos 注册
            CompletableFuture.runAsync(() -> {
                logger.info("Starting AI connection warmup...");
                long startTime = System.currentTimeMillis();

                try {
                    ChatClient chatClient = chatClientBuilder.build();
                    chatClient.prompt()
                            .messages(new UserMessage("ping"))
                            .stream()
                            .content()
                            .collectList()
                            .block(Duration.ofSeconds(15));

                    logger.info("AI connection warmup completed in {}ms", System.currentTimeMillis() - startTime);
                } catch (Exception e) {
                    logger.warn("AI connection warmup failed (will retry on first request): {}", e.getMessage());
                }
            });
        };
    }

    private String normalizeBaseUrl(String rawBaseUrl) {
        if (rawBaseUrl == null) {
            return "https://dashscope.aliyuncs.com/compatible-mode";
        }

        String normalized = rawBaseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.endsWith("/v1")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }

        return normalized;
    }
}
