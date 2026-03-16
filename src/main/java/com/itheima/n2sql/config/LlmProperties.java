package com.itheima.n2sql.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM 大模型配置属性类
 *
 * 自动从 application-dev.yml 中读取以 "n2sql.llm" 开头的配置项。
 * 比如 yml 中写：
 *   n2sql:
 *     llm:
 *       api-key: sk-xxx
 *       base-url: https://api.deepseek.com/v1
 *
 * Spring 会自动把这些值填充到这个类的对应字段中。
 *
 * @ConfigurationProperties 注解：告诉 Spring "去 yml 里找 n2sql.llm 开头的配置"
 * yml 中的 api-key（横杠命名）会自动映射到 Java 的 apiKey（驼峰命名）
 */
@Data
@Component
@ConfigurationProperties(prefix = "n2sql.llm")
public class LlmProperties {

    /** API 密钥（必填，从大模型厂商官网获取） */
    private String apiKey = "YOUR_API_KEY_HERE";

    /** API 基础地址（不同厂商地址不同） */
    private String baseUrl = "https://api.deepseek.com/v1";

    /** 模型名称（如 deepseek-chat、glm-4、gpt-4o） */
    private String modelName = "deepseek-chat";

    /**
     * 温度参数（0.0 ~ 1.0）
     * 越低 = 回答越确定、越稳定（适合生成 SQL）
     * 越高 = 回答越随机、越有创意（适合写故事）
     * 生成 SQL 建议设为 0.0，要求精确不要创意
     */
    private double temperature = 0.0;

    /** 最大输出 token 数（限制大模型回复的长度） */
    private int maxTokens = 2048;
}
