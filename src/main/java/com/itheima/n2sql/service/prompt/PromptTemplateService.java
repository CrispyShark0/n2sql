package com.itheima.n2sql.service.prompt;

import com.itheima.n2sql.model.dto.CorrectionRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 提示词模板服务
 *
 * 负责：
 *   1. 从 resources/prompts/ 目录加载提示词模板文件
 *   2. 把模板中的 {{变量}} 替换为真实值
 *   3. 返回最终的完整提示词，交给大模型处理
 *
 * 【改进说明 — 按老师要求】
 *   - buildSoftPrompt()：软策略，基础模板 + 动态追加补充指令（替代旧的硬分类模板选择）
 *   - buildCorrectionPromptWithHistory()：纠错时传入完整历史错误上下文（替代旧的单次纠错）
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
     */
    @PostConstruct
    public void init() {
        // 加载基础 NL2SQL 提示词模板（软策略的核心模板，包含 {{hints}} 占位符）
        loadTemplate("base_nl2sql", "prompts/base_nl2sql.txt");
        // 加载自纠错提示词模板（包含 {{error_history}} 占位符，支持完整历史）
        loadTemplate("correction_nl2sql", "prompts/correction_nl2sql.txt");
        log.info("提示词模板加载完成，共 {} 个模板", templateCache.size());
    }

    /**
     * 从 resources 目录加载一个模板文件
     */
    private void loadTemplate(String name, String filePath) {
        try {
            ClassPathResource resource = new ClassPathResource(filePath);
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            templateCache.put(name, content);
            log.debug("模板 [{}] 加载成功，长度 {} 字符", name, content.length());
        } catch (IOException e) {
            log.error("模板 [{}] 加载失败: {}", name, e.getMessage());
        }
    }

    /**
     * 【核心方法 — 软策略】构建带动态补充指令的提示词
     *
     * 始终使用 base_nl2sql 基础模板，然后把 QueryClassifier 检测到的 hints 动态追加进去。
     * 模板中的 {{hints}} 占位符会被替换为拼接好的补充指令文本。
     *
     * @param schemaDDL 数据库结构 DDL
     * @param question  用户问题
     * @param dbType    数据库类型（如 "MySQL"、"PostgreSQL"），可为 null
     * @param hints     QueryClassifier 检测到的补充指令列表
     * @return 填充好的完整提示词
     */
    public String buildSoftPrompt(String schemaDDL, String question,
                                  String dbType, List<String> hints) {
        String template = templateCache.get("base_nl2sql");
        if (template == null) {
            throw new RuntimeException("base_nl2sql 提示词模板未加载");
        }

        // 把 hints 列表拼接成一个文本块
        String hintsText = (hints != null && !hints.isEmpty())
                ? String.join("\n", hints)
                : "";

        // 替换模板中的占位符
        String prompt = template
                .replace("{{schema}}", schemaDDL)
                .replace("{{hints}}", hintsText)
                .replace("{{question}}", question);

        // 追加数据库方言提示
        if (dbType != null && !dbType.isEmpty()) {
            prompt = prompt + "\n\nIMPORTANT: Generate SQL compatible with " + dbType
                    + " syntax. Use " + dbType + "-specific functions and features when appropriate.";
        }

        log.debug("构建软策略提示词完成，hints 数量: {}，总长度 {} 字符",
                (hints != null ? hints.size() : 0), prompt.length());
        return prompt;
    }

    /**
     * 【核心方法 — 完整历史纠错】构建带完整错误历史的纠错提示词
     *
     * 按老师要求：每次重试时，把之前所有失败的 SQL 和对应的错误信息都传给大模型，
     * 这样模型能看到"我之前尝试过什么、分别哪里出错了"，避免来回振荡犯同样的错误。
     *
     * @param schemaDDL         数据库结构 DDL
     * @param question          用户原始问题
     * @param correctionHistory 完整的纠错历史记录列表
     * @return 填充好的纠错提示词
     */
    public String buildCorrectionPromptWithHistory(String schemaDDL, String question,
                                                   List<CorrectionRecord> correctionHistory) {
        String template = templateCache.get("correction_nl2sql");
        if (template == null) {
            throw new RuntimeException("correction_nl2sql 提示词模板未加载");
        }

        // 格式化完整的错误历史
        StringBuilder historyText = new StringBuilder();
        for (int i = 0; i < correctionHistory.size(); i++) {
            CorrectionRecord record = correctionHistory.get(i);
            historyText.append("### Attempt ").append(i + 1).append("\n");
            historyText.append("SQL:\n```sql\n").append(record.getSql()).append("\n```\n");
            historyText.append("Error Type: ").append(record.getErrorType()).append("\n");
            historyText.append("Error Message: ").append(record.getErrorMessage()).append("\n\n");
        }

        // 替换模板中的占位符
        String prompt = template
                .replace("{{schema}}", schemaDDL)
                .replace("{{question}}", question)
                .replace("{{error_history}}", historyText.toString());

        log.debug("构建完整历史纠错提示词完成，历史记录 {} 条，总长度 {} 字符",
                correctionHistory.size(), prompt.length());
        return prompt;
    }

    /**
     * 获取原始模板内容（用于调试/查看）
     */
    public String getTemplate(String name) {
        return templateCache.get(name);
    }
}
