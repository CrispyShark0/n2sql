package com.itheima.n2sql.service;

import com.itheima.n2sql.model.dto.CorrectionRecord;
import com.itheima.n2sql.model.dto.DatabaseSchema;
import com.itheima.n2sql.model.dto.Nl2SqlResponse;
import com.itheima.n2sql.model.dto.QueryResult;
import com.itheima.n2sql.service.prompt.PromptTemplateService;
import com.itheima.n2sql.util.SqlCleanUtil;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * NL2SQL 核心转换服务 — 项目的"心脏"
 *
 * 【改进说明 — 按老师要求】
 *   改进1：软策略 — 用基础 prompt 兜底 + 动态追加补充指令（不再硬分类选模板）
 *   改进2：完整历史 — 纠错时传入之前所有失败的 SQL 和错误信息（避免来回振荡）
 *   改进3：NO_MATCH — 不存在的实体不猜测，返回 NO_MATCH 说明理由
 *
 * 完整流程：
 *   用户问题 + 数据源ID
 *       ↓
 *   ① 提取数据库 Schema
 *       ↓
 *   ② 检测查询关键词 → 生成补充指令(hints)
 *       ↓
 *   ③ 基础模板 + hints → 调用大模型 → 清洗响应
 *       ↓
 *   ④ 检查 NO_MATCH → 如果是，直接返回友好提示
 *       ↓
 *   ⑤ 静态验证（JSQLParser语法 + Schema校验）
 *       ↓ 失败 → 记录到历史 → 带完整历史纠错 → 重新调用大模型（回到③）
 *   ⑥ 动态验证：数据库真实执行 SQL
 *       ↓ 失败 → 记录到历史 → 带完整历史纠错 → 重新调用大模型（回到③）
 *   ⑦ 返回成功结果
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class  Nl2SqlService {

    /** Schema 提取服务 */
    private final SchemaExtractService schemaExtractService;

    /** 提示词模板服务 */
    private final PromptTemplateService promptTemplateService;

    /** 大模型客户端 */
    private final ChatLanguageModel chatLanguageModel;

    /** SQL 执行引擎 */
    private final SqlExecuteService sqlExecuteService;

    /** SQL 语法静态验证服务 */
    private final SqlValidateService sqlValidateService;

    /** 查询意图检测器（软策略版本） */
    private final QueryClassifier queryClassifier;

    /** 最大纠错重试次数 */
    @Value("${n2sql.correction.max-retries:3}")
    private int maxRetries;

    /**
     * 核心方法：将自然语言问题转换为 SQL（带自纠错 + 软策略 + NO_MATCH）
     */
    public Nl2SqlResponse generateSql(String dataSourceId, String question) {
        log.info("开始 NL2SQL 转换 | 数据源: {} | 问题: {}", dataSourceId, question);

        // 纠错历史记录（记录每轮的SQL和错误，用于完整历史纠错）
        List<CorrectionRecord> correctionHistory = new ArrayList<>();

        try {
            // ① 提取数据库结构（Schema）
            DatabaseSchema schema = schemaExtractService.extractSchema(dataSourceId);
            String schemaDDL = schema.formatAsDDL();
            log.debug("Schema DDL 长度: {} 字符", schemaDDL.length());

            // ② 【改进：软策略】检测查询关键词，生成补充指令
            List<String> hints = queryClassifier.detectHints(question, schema);
            log.info("软策略检测完成，共 {} 条补充指令", hints.size());

            // ③ 用基础模板 + hints 构建提示词，调用大模型
            String prompt = promptTemplateService.buildSoftPrompt(
                    schemaDDL, question, schema.getDbType(), hints);
            log.info("正在调用大模型（首次生成，软策略，方言: {}）...", schema.getDbType());
            String rawResponse = chatLanguageModel.generate(prompt);
            log.info("大模型返回原始文本: {}", rawResponse);

            // 清洗响应
            String currentSql = SqlCleanUtil.cleanSql(rawResponse);
            log.info("清洗后的 SQL: {}", currentSql);

            // ④ 【改进：NO_MATCH 检测】检查大模型是否返回了 NO_MATCH
            if (isNoMatch(currentSql) || isNoMatch(rawResponse)) {
                String noMatchReason = extractNoMatchReason(
                        isNoMatch(currentSql) ? currentSql : rawResponse);
                log.info("大模型返回 NO_MATCH: {}", noMatchReason);
                return Nl2SqlResponse.builder()
                        .question(question)
                        .success(false)
                        .errorMessage("查询无法执行：" + noMatchReason)
                        .correctionHistory(correctionHistory)
                        .build();
            }

            // ⑤⑥ 进入「验证 → 执行 → 纠错」循环
            for (int attempt = 0; attempt <= maxRetries; attempt++) {

                // --- 第一关：静态验证（语法 + Schema校验） ---
                SqlValidateService.ValidationResult validateResult =
                        sqlValidateService.validate(currentSql, schema);

                if (!validateResult.isValid()) {
                    String errorMsg = validateResult.getErrorMessage();
                    log.warn("第{}轮 | 语法验证失败: {}", attempt + 1, errorMsg);

                    // 记录这轮纠错历史
                    correctionHistory.add(CorrectionRecord.builder()
                            .sql(currentSql)
                            .errorType("SYNTAX_ERROR")
                            .errorMessage(errorMsg)
                            .build());

                    if (attempt < maxRetries) {
                        // 【改进：完整历史纠错】传入完整的 correctionHistory
                        currentSql = requestCorrectionWithHistory(
                                schemaDDL, question, correctionHistory);

                        // 纠错后也要检查 NO_MATCH
                        if (isNoMatch(currentSql)) {
                            String noMatchReason = extractNoMatchReason(currentSql);
                            log.info("纠错后大模型返回 NO_MATCH: {}", noMatchReason);
                            return Nl2SqlResponse.builder()
                                    .question(question)
                                    .success(false)
                                    .errorMessage("查询无法执行：" + noMatchReason)
                                    .correctionHistory(correctionHistory)
                                    .build();
                        }
                        continue;
                    } else {
                        log.error("达到最大重试次数 {}，语法验证仍未通过", maxRetries);
                        return buildFailResponse(question, currentSql,
                                errorMsg, correctionHistory);
                    }
                }

                // --- 第二关：数据库真实执行 ---
                try {
                    QueryResult queryResult =
                            sqlExecuteService.execute(dataSourceId, currentSql);

                    // 执行成功！
                    log.info("SQL 执行成功！重试次数: {}", attempt);
                    return Nl2SqlResponse.builder()
                            .question(question)
                            .generatedSql(currentSql)
                            .queryResult(queryResult)
                            .success(true)
                            .retryCount(attempt)
                            .correctionHistory(correctionHistory)
                            .build();

                } catch (Exception execEx) {
                    String errorMsg = "SQL 执行失败: " + execEx.getMessage();
                    log.warn("第{}轮 | 执行失败: {}", attempt + 1, execEx.getMessage());

                    correctionHistory.add(CorrectionRecord.builder()
                            .sql(currentSql)
                            .errorType("EXECUTION_ERROR")
                            .errorMessage(errorMsg)
                            .build());

                    if (attempt < maxRetries) {
                        // 【改进：完整历史纠错】传入完整的 correctionHistory
                        currentSql = requestCorrectionWithHistory(
                                schemaDDL, question, correctionHistory);

                        // 纠错后也要检查 NO_MATCH
                        if (isNoMatch(currentSql)) {
                            String noMatchReason = extractNoMatchReason(currentSql);
                            log.info("纠错后大模型返回 NO_MATCH: {}", noMatchReason);
                            return Nl2SqlResponse.builder()
                                    .question(question)
                                    .success(false)
                                    .errorMessage("查询无法执行：" + noMatchReason)
                                    .correctionHistory(correctionHistory)
                                    .build();
                        }
                    } else {
                        log.error("达到最大重试次数 {}，SQL 执行仍然失败", maxRetries);
                        return buildFailResponse(question, currentSql,
                                errorMsg, correctionHistory);
                    }
                }
            }

            return buildFailResponse(question, currentSql, "未知错误", correctionHistory);

        } catch (Exception e) {
            log.error("NL2SQL 转换失败: ", e);
            return Nl2SqlResponse.builder()
                    .question(question)
                    .success(false)
                    .errorMessage("SQL 生成失败: " + e.getMessage())
                    .correctionHistory(correctionHistory)
                    .build();
        }
    }

    /**
     * 【改进】请求大模型修正 SQL — 传入完整的历史错误上下文
     *
     * 按老师要求：每次重试时，把之前所有失败的 SQL 和对应的错误信息都传给大模型，
     * 让模型看到"我之前尝试过什么、分别哪里出错了"，避免来回振荡犯同样的错误。
     *
     * @param schemaDDL         数据库结构DDL
     * @param question          用户原始问题
     * @param correctionHistory 完整的纠错历史（所有之前的失败尝试）
     * @return 大模型修正后的新SQL（已清洗）
     */
    private String requestCorrectionWithHistory(String schemaDDL, String question,
                                                List<CorrectionRecord> correctionHistory) {
        log.info("正在请求大模型纠错（第{}次重试，传入{}条历史记录）...",
                correctionHistory.size(), correctionHistory.size());

        // 用完整历史构建纠错提示词
        String correctionPrompt = promptTemplateService.buildCorrectionPromptWithHistory(
                schemaDDL, question, correctionHistory);

        // 调用大模型
        String rawResponse = chatLanguageModel.generate(correctionPrompt);
        log.info("大模型纠错返回: {}", rawResponse);

        // 清洗SQL
        String correctedSql = SqlCleanUtil.cleanSql(rawResponse);
        log.info("纠错后清洗的 SQL: {}", correctedSql);

        return correctedSql;
    }

    /**
     * 【改进】检查大模型返回是否为 NO_MATCH（实体不存在）
     *
     * 按老师要求：如果用户问了不存在的实体（如"查询学生"，但数据库只有员工表），
     * 大模型应返回 NO_MATCH: <原因>，系统不应猜测。
     */
    private boolean isNoMatch(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.trim().toUpperCase().startsWith("NO_MATCH");
    }

    /**
     * 从 NO_MATCH 响应中提取原因
     */
    private String extractNoMatchReason(String text) {
        if (text == null) {
            return "未知原因";
        }
        String trimmed = text.trim();
        int colonIndex = trimmed.indexOf(':');
        if (colonIndex >= 0 && colonIndex < trimmed.length() - 1) {
            return trimmed.substring(colonIndex + 1).trim();
        }
        return trimmed;
    }

    /**
     * 构建失败响应
     */
    private Nl2SqlResponse buildFailResponse(String question, String lastSql,
                                             String errorMessage,
                                             List<CorrectionRecord> history) {
        return Nl2SqlResponse.builder()
                .question(question)
                .generatedSql(lastSql)
                .success(false)
                .errorMessage(errorMessage)
                .retryCount(history.size())
                .correctionHistory(history)
                .build();
    }
}
