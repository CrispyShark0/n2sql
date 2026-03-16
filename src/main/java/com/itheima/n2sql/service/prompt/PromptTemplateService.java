package com.itheima.n2sql.service.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 提示词模板服务
 *
 * 负责：
 *   1. 从 resources/prompts/ 目录加载提示词模板文件
 *   2. 把模板中的 {{变量}} 替换为真实值
 *   3. 返回最终的完整提示词，交给大模型处理
 *
 * 简单理解：
 *   模板文件 = 一封有空格的信
 *   这个服务 = 帮你填空格，把信写完整
 *
 * @PostConstruct 注解：Spring 创建这个对象后，自动调用被标注的方法（用来初始化）
 */
@Slf4j
@Service
public class PromptTemplateService {

    /**
     * 缓存所有已加载的模板
     * Key = 模板名称（如 "base_nl2sql"）
     * Value = 模板内容（带 {{变量}} 占位符的文本）
     */
    private final Map<String, String> templateCache = new HashMap<>();

    /**
     * 项目启动时自动加载所有模板文件
     *
     * @PostConstruct 表示 Spring 创建好这个对象后自动调用此方法
     */
    @PostConstruct
    public void init() {
        // 加载基础 NL2SQL 提示词模板（兜底用）
        loadTemplate("base_nl2sql", "prompts/base_nl2sql.txt");
        // 加载自纠错提示词模板（第三阶段新增）
        loadTemplate("correction_nl2sql", "prompts/correction_nl2sql.txt");
        // 加载分类专用提示词模板（第四阶段新增）
        loadTemplate("simple_nl2sql", "prompts/simple_nl2sql.txt");
        loadTemplate("aggregate_nl2sql", "prompts/aggregate_nl2sql.txt");
        loadTemplate("multijoin_nl2sql", "prompts/multijoin_nl2sql.txt");
        loadTemplate("nested_nl2sql", "prompts/nested_nl2sql.txt");
        log.info("提示词模板加载完成，共 {} 个模板", templateCache.size());
    }

    /**
     * 从 resources 目录加载一个模板文件
     *
     * @param name     模板名称（自定义的标识符）
     * @param filePath 文件路径（相对于 resources 目录）
     */
    private void loadTemplate(String name, String filePath) {
        try {
            // ClassPathResource 可以读取 resources 目录下的文件
            ClassPathResource resource = new ClassPathResource(filePath);
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            templateCache.put(name, content);
            log.debug("模板 [{}] 加载成功，长度 {} 字符", name, content.length());
        } catch (IOException e) {
            log.error("模板 [{}] 加载失败: {}", name, e.getMessage());
        }
    }

    /**
     * 构建基础 NL2SQL 提示词
     *
     * 把数据库的 Schema（DDL格式）和用户问题填入模板，生成完整的提示词。
     *
     * @param schemaDDL 数据库结构的 DDL 文本（由 DatabaseSchema.formatAsDDL() 生成）
     * @param question  用户的自然语言问题（如 "查询所有用户"）
     * @return 填充好的完整提示词
     */
    public String buildNl2SqlPrompt(String schemaDDL, String question) {
        return buildPromptByTemplateName("base_nl2sql", schemaDDL, question);
    }

    /**
     * 根据指定模板名称构建提示词（第四阶段新增）
     *
     * 由 QueryClassifier 分类后，传入对应的模板名称。
     * 例如：AGGREGATE 类型 → templateName = "aggregate_nl2sql"
     *
     * @param templateName 模板名称（对应 QueryType.getTemplateName()）
     * @param schemaDDL    数据库结构 DDL
     * @param question     用户问题
     * @return 填充好的提示词
     */
    public String buildPromptByTemplateName(String templateName, String schemaDDL, String question) {
        return buildPromptByTemplateName(templateName, schemaDDL, question, null);
    }

    /**
     * 根据模板名称构建提示词（带数据库方言，第五阶段新增）
     *
     * @param templateName 模板名称
     * @param schemaDDL    数据库结构 DDL
     * @param question     用户问题
     * @param dbType       数据库类型（如 "MySQL"、"PostgreSQL"），可为null
     * @return 填充好的提示词
     */
    public String buildPromptByTemplateName(String templateName, String schemaDDL,
                                            String question, String dbType) {
        String template = templateCache.get(templateName);
        if (template == null) {
            // 找不到指定模板，回退到基础模板
            log.warn("模板 [{}] 未找到，回退到 base_nl2sql", templateName);
            template = templateCache.get("base_nl2sql");
        }
        if (template == null) {
            throw new RuntimeException("base_nl2sql 提示词模板未加载");
        }

        // 替换模板中的占位符
        String prompt = template
                .replace("{{schema}}", schemaDDL)
                .replace("{{question}}", question);

        // 如果有数据库类型信息，在提示词末尾追加方言提示（第五阶段新增）
        if (dbType != null && !dbType.isEmpty()) {
            prompt = prompt + "\n\nIMPORTANT: Generate SQL compatible with " + dbType
                    + " syntax. Use " + dbType + "-specific functions and features when appropriate.";
        }

        log.debug("构建提示词完成 [{}]，总长度 {} 字符", templateName, prompt.length());
        return prompt;
    }

    /**
     * 构建自纠错提示词（第三阶段新增）
     *
     * 当大模型生成的 SQL 有问题时（语法错误或执行报错），
     * 把错误信息"喂"回给大模型，让它参考错误信息修正 SQL。
     *
     * 就像你写代码报错了，看着报错信息去改一样。
     *
     * @param schemaDDL    数据库结构的 DDL 文本
     * @param question     用户的原始自然语言问题
     * @param previousSql  上一轮生成的错误 SQL
     * @param errorMessage 错误信息（语法错误或数据库执行错误）
     * @return 填充好的纠错提示词
     */
    public String buildCorrectionPrompt(String schemaDDL, String question,
                                        String previousSql, String errorMessage) {
        String template = templateCache.get("correction_nl2sql");
        if (template == null) {
            throw new RuntimeException("correction_nl2sql 提示词模板未加载");
        }

        // 替换模板中的4个占位符
        String prompt = template
                .replace("{{schema}}", schemaDDL)
                .replace("{{question}}", question)
                .replace("{{previous_sql}}", previousSql)
                .replace("{{error_message}}", errorMessage);

        log.debug("构建纠错提示词完成，总长度 {} 字符", prompt.length());
        return prompt;
    }

    /**
     * 获取原始模板内容（用于调试/查看）
     *
     * @param name 模板名称
     * @return 模板原始内容
     */
    public String getTemplate(String name) {
        return templateCache.get(name);
    }
}
