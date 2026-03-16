package com.itheima.n2sql.service;

import com.itheima.n2sql.model.dto.CorrectionRecord;
import com.itheima.n2sql.model.dto.DatabaseSchema;
import com.itheima.n2sql.model.dto.Nl2SqlResponse;
import com.itheima.n2sql.model.dto.QueryResult;
import com.itheima.n2sql.model.enums.QueryType;
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
 * 第三阶段升级：加入了自纠错反馈闭环机制！
 *
 * 完整流程（带纠错）：
 *   用户问题 + 数据源ID
 *       ↓
 *   ① 提取数据库 Schema
 *       ↓
 *   ② 构建提示词 → 调用大模型 → 清洗SQL
 *       ↓
 *   ③ 静态验证（两级）：JSQLParser语法检查 + Schema表名/列名校验
 *       ↓ 失败 → 记录错误 → 构建纠错提示词 → 重新调用大模型（回到②）
 *   ④ 动态验证：数据库真实执行 SQL
 *       ↓ 失败 → 记录错误 → 构建纠错提示词 → 重新调用大模型（回到②）
 *   ⑤ 返回成功结果（或达到最大重试次数后返回最后的错误）
 *
 * 纠错循环最多执行 maxRetries 次（yml 配置，默认3次）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Nl2SqlService {

    /** Schema 提取服务 */
    private final SchemaExtractService schemaExtractService;

    /** 提示词模板服务 */
    private final PromptTemplateService promptTemplateService;

    /** 大模型客户端（由 LlmConfig 创建，Spring 自动注入） */
    private final ChatLanguageModel chatLanguageModel;

    /** SQL 执行引擎 */
    private final SqlExecuteService sqlExecuteService;

    /** SQL 语法静态验证服务（第三阶段新增） */
    private final SqlValidateService sqlValidateService;

    /** 查询意图分类器（第四阶段新增） */
    private final QueryClassifier queryClassifier;

    /** 最大纠错重试次数，从 yml 配置读取，默认 3 次 */
    @Value("${n2sql.correction.max-retries:3}")
    private int maxRetries;

    /**
     * 核心方法：将自然语言问题转换为 SQL（带自纠错）
     *
     * @param dataSourceId 数据源ID（在哪个数据库上查询）
     * @param question     用户的自然语言问题
     * @return Nl2SqlResponse 包含生成的SQL、执行结果、纠错历史
     */
    public Nl2SqlResponse generateSql(String dataSourceId, String question) {
        log.info("开始 NL2SQL 转换 | 数据源: {} | 问题: {}", dataSourceId, question);

        // 纠错历史记录（记录每轮的SQL和错误，方便调试和前端展示）
        List<CorrectionRecord> correctionHistory = new ArrayList<>();

        try {
            // ① 提取数据库结构（Schema），并格式化为 DDL 文本
            //    这一步只需要做一次，后续纠错时复用同一份 Schema
            DatabaseSchema schema = schemaExtractService.extractSchema(dataSourceId);
            String schemaDDL = schema.formatAsDDL();
            log.debug("Schema DDL 长度: {} 字符", schemaDDL.length());

            // ② 查询意图分类（第四阶段新增）
            //    根据用户问题类型，选择最合适的提示词模板
            QueryType queryType = queryClassifier.classify(question, schema);
            log.info("查询意图分类: {} ({})", queryType.name(), queryType.getDescription());

            // ③ 用分类对应的模板构建提示词，调用大模型生成 SQL
            //    同时传入数据库类型，让大模型生成对应方言的 SQL（第五阶段新增）
            String prompt = promptTemplateService.buildPromptByTemplateName(
                    queryType.getTemplateName(), schemaDDL, question, schema.getDbType());
            log.info("正在调用大模型（首次生成，模板: {}，方言: {}）...",
                    queryType.getTemplateName(), schema.getDbType());
            String rawResponse = chatLanguageModel.generate(prompt);
            log.info("大模型返回原始文本: {}", rawResponse);

            // 清洗 SQL（去掉 markdown 格式、多余文字等）
            String currentSql = SqlCleanUtil.cleanSql(rawResponse);
            log.info("清洗后的 SQL: {}", currentSql);

            // ③ 进入「验证 → 执行 → 纠错」循环
            //    attempt=0 表示首次生成的结果
            //    attempt=1,2,3... 表示第1,2,3次纠错后的结果
            for (int attempt = 0; attempt <= maxRetries; attempt++) {

                // --- 第一关：静态验证（语法 + Schema校验） ---
                SqlValidateService.ValidationResult validateResult =
                        sqlValidateService.validate(currentSql, schema);

                if (!validateResult.isValid()) {
                    // 语法验证失败
                    String errorMsg = validateResult.getErrorMessage();
                    log.warn("第{}轮 | 语法验证失败: {}", attempt + 1, errorMsg);

                    // 记录这轮纠错历史
                    correctionHistory.add(CorrectionRecord.builder()
                            .sql(currentSql)
                            .errorType("SYNTAX_ERROR")
                            .errorMessage(errorMsg)
                            .build());

                    // 如果还有重试次数，就让大模型修正
                    if (attempt < maxRetries) {
                        currentSql = requestCorrection(
                                schemaDDL, question, currentSql, errorMsg, attempt + 1);
                        continue;  // 回到循环开头，重新验证修正后的SQL
                    } else {
                        // 已达到最大重试次数，返回失败
                        log.error("达到最大重试次数 {}，语法验证仍未通过", maxRetries);
                        return buildFailResponse(question, currentSql,
                                errorMsg, correctionHistory);
                    }
                }

                // --- 第二关：数据库真实执行 ---
                try {
                    QueryResult queryResult =
                            sqlExecuteService.execute(dataSourceId, currentSql);

                    // 执行成功！返回结果
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
                    // 执行失败（比如列名不存在、表名错误等）
                    // 这些问题 JSQLParser 查不出来，只有数据库知道
                    String errorMsg = "SQL 执行失败: " + execEx.getMessage();
                    log.warn("第{}轮 | 执行失败: {}", attempt + 1, execEx.getMessage());

                    // 记录纠错历史
                    correctionHistory.add(CorrectionRecord.builder()
                            .sql(currentSql)
                            .errorType("EXECUTION_ERROR")
                            .errorMessage(errorMsg)
                            .build());

                    // 如果还有重试次数，就让大模型修正
                    if (attempt < maxRetries) {
                        currentSql = requestCorrection(
                                schemaDDL, question, currentSql, errorMsg, attempt + 1);
                        // 不需要 continue，for 循环会自动回到开头
                    } else {
                        // 已达到最大重试次数，返回失败
                        log.error("达到最大重试次数 {}，SQL 执行仍然失败", maxRetries);
                        return buildFailResponse(question, currentSql,
                                errorMsg, correctionHistory);
                    }
                }
            }

            // 理论上不会走到这里（循环内部一定会 return），但作为兜底
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
     * 请求大模型修正 SQL（自纠错的核心私有方法）
     *
     * 构建一个纠错提示词，把上一轮的错误SQL和错误信息都告诉大模型，
     * 让它"看着错误信息来修改"。
     *
     * @param schemaDDL    数据库结构DDL
     * @param question     用户原始问题
     * @param previousSql  上一轮的错误SQL
     * @param errorMessage 错误信息
     * @param retryNum     当前是第几次重试（用于日志）
     * @return 大模型修正后的新SQL（已清洗）
     */
    private String requestCorrection(String schemaDDL, String question,
                                     String previousSql, String errorMessage,
                                     int retryNum) {
        log.info("正在请求大模型纠错（第{}次重试）...", retryNum);

        // 用纠错模板构建提示词（比基础模板多了 previousSql 和 errorMessage）
        String correctionPrompt = promptTemplateService.buildCorrectionPrompt(
                schemaDDL, question, previousSql, errorMessage);

        // 调用大模型
        String rawResponse = chatLanguageModel.generate(correctionPrompt);
        log.info("大模型纠错返回: {}", rawResponse);

        // 清洗SQL
        String correctedSql = SqlCleanUtil.cleanSql(rawResponse);
        log.info("纠错后清洗的 SQL: {}", correctedSql);

        return correctedSql;
    }

    /**
     * 构建失败响应（抽取公共代码，避免重复写）
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
