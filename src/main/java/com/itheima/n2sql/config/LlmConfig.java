package com.itheima.n2sql.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LLM 大模型配置类
 *
 * 根据 yml 配置文件中的参数，创建大模型客户端对象。
 * 创建好之后，其他类只需要 @Autowired 注入 ChatLanguageModel 就能直接使用。
 *
 * @Configuration 注解：告诉 Spring "这个类里面有 @Bean 方法，要注册到容器中"
 * @Bean 注解：告诉 Spring "这个方法的返回值是一个需要管理的对象"
 *
 * 简单理解：
 *   @Configuration = 工厂
 *   @Bean = 工厂生产的产品
 *   Spring 会自动调用 @Bean 方法，把返回的对象存起来，其他类要用时自动注入
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class LlmConfig {

    /** LLM 配置属性（从 yml 中自动读取） */
    private final LlmProperties llmProperties;

    /**
     * 创建大模型客户端 Bean
     *
     * 使用 LangChain4j 的 OpenAiChatModel，因为 DeepSeek/智谱 都兼容 OpenAI 的 API 格式。
     * 只需要修改 baseUrl 和 apiKey 就能切换不同厂商。
     *
     * @return ChatLanguageModel 大模型客户端，可以在任何地方注入使用
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("正在初始化 LLM 大模型客户端...");
        log.info("  模型: {}", llmProperties.getModelName());
        log.info("  地址: {}", llmProperties.getBaseUrl());
        log.info("  温度: {}", llmProperties.getTemperature());

        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey(llmProperties.getApiKey())
                .baseUrl(llmProperties.getBaseUrl())
                .modelName(llmProperties.getModelName())
                .temperature(llmProperties.getTemperature())
                .maxTokens(llmProperties.getMaxTokens())
                .timeout(Duration.ofSeconds(60))   // 超时时间 60 秒（复杂SQL生成可能较慢）
                .logRequests(true)                  // 开发阶段打印请求日志，方便调试
                .logResponses(true)                 // 开发阶段打印响应日志
                .build();

        log.info("LLM 大模型客户端初始化完成！");
        return model;
    }
}
