package com.itheima.n2sql.controller;

import com.itheima.n2sql.model.dto.*;
import com.itheima.n2sql.service.*;
import com.itheima.n2sql.service.prompt.PromptTemplateService;
import com.itheima.n2sql.util.SqlCleanUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 校验机制调试接口 — 仅用于测试，绕过大模型直接测试各个校验环节
 *
 * 接口列表：
 *   POST /api/debug/validate        — 直接测试静态校验（JSQLParser + Schema校验）
 *   POST /api/debug/execute          — 直接测试SQL执行（跳过大模型，直接送SQL到数据库）
 *   POST /api/debug/full-pipeline    — 测试完整流水线（手动指定SQL，走校验→执行→纠错全流程）
 *   GET  /api/debug/schema/{dsId}    — 查看提取的Schema DDL（调试用）
 */
@Slf4j
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final SchemaExtractService schemaExtractService;
    private final SqlValidateService sqlValidateService;
    private final SqlExecuteService sqlExecuteService;

    // ====== 请求体 ======
    @Data
    public static class ValidateRequest {
        /** 数据源ID */
        private String dataSourceId;
        /** 要校验的SQL语句 */
        private String sql;
    }

    @Data
    public static class ExecuteRequest {
        /** 数据源ID */
        private String dataSourceId;
        /** 要执行的SQL语句 */
        private String sql;
    }

    @Data
    public static class PipelineRequest {
        /** 数据源ID */
        private String dataSourceId;
        /** 手动指定的SQL列表（模拟大模型每轮生成的SQL） */
        private List<String> sqlSequence;
    }

    // ====== 响应体 ======
    @Data
    public static class ValidateResponse {
        private String sql;
        private boolean valid;
        private String errorMessage;
        private String errorCategory; // SYNTAX_ERROR / TABLE_NOT_FOUND / COLUMN_NOT_FOUND / NON_SELECT / OK
    }

    @Data
    public static class PipelineResponse {
        private List<StepResult> steps = new ArrayList<>();
        private boolean finalSuccess;
        private String finalSql;
        private QueryResult queryResult;
        private int totalRetries;
    }

    @Data
    public static class StepResult {
        private int attempt;
        private String sql;
        private String stage; // STATIC_VALIDATE / EXECUTE
        private boolean passed;
        private String errorType;
        private String errorMessage;
    }

    // ====== 接口实现 ======

    /**
     * 直接测试静态校验
     * 绕过大模型，手动送SQL进来看校验结果
     */
    @PostMapping("/validate")
    public ApiResult<ValidateResponse> validate(@RequestBody ValidateRequest req) {
        log.info("[DEBUG] 静态校验测试 | SQL: {}", req.getSql());

        DatabaseSchema schema = schemaExtractService.extractSchema(req.getDataSourceId());
        SqlValidateService.ValidationResult result = sqlValidateService.validate(req.getSql(), schema);

        ValidateResponse resp = new ValidateResponse();
        resp.setSql(req.getSql());
        resp.setValid(result.isValid());
        resp.setErrorMessage(result.getErrorMessage());

        // 分类错误类型（注意顺序：列名错误的消息也包含"表"字，所以必须先匹配列名）
        if (result.isValid()) {
            resp.setErrorCategory("OK");
        } else {
            String msg = result.getErrorMessage();
            if (msg.contains("语法错误")) resp.setErrorCategory("SYNTAX_ERROR");
            else if (msg.contains("不是 SELECT")) resp.setErrorCategory("NON_SELECT");
            else if (msg.startsWith("列")) resp.setErrorCategory("COLUMN_NOT_FOUND");
            else if (msg.startsWith("表")) resp.setErrorCategory("TABLE_NOT_FOUND");
            else resp.setErrorCategory("OTHER_ERROR");
        }

        log.info("[DEBUG] 校验结果 | valid={} | category={} | msg={}", resp.isValid(), resp.getErrorCategory(), resp.getErrorMessage());
        return ApiResult.success(resp);
    }

    /**
     * 直接测试SQL执行
     * 跳过静态校验，直接送到数据库执行
     */
    @PostMapping("/execute")
    public ApiResult<Object> execute(@RequestBody ExecuteRequest req) {
        log.info("[DEBUG] SQL执行测试 | SQL: {}", req.getSql());

        // 安全检查仍然保留
        if (!SqlCleanUtil.isSelectStatement(req.getSql())) {
            return ApiResult.error("安全限制：只允许执行 SELECT 查询语句");
        }

        try {
            QueryResult result = sqlExecuteService.execute(req.getDataSourceId(), req.getSql());
            log.info("[DEBUG] 执行成功 | {}行 | {}ms", result.getRowCount(), result.getExecuteTimeMs());
            return ApiResult.success(result);
        } catch (Exception e) {
            log.warn("[DEBUG] 执行失败 | {}", e.getMessage());
            return ApiResult.error("EXECUTION_ERROR: " + e.getMessage());
        }
    }

    /**
     * 测试完整流水线（手动模拟多轮纠错）
     *
     * sqlSequence 里放多个SQL，模拟大模型每轮生成的SQL：
     *   第0个 = 首次生成的SQL
     *   第1个 = 第1次纠错后的SQL
     *   第2个 = 第2次纠错后的SQL
     *   ...
     * 系统会按顺序走 静态校验→执行，失败就取下一个SQL继续，直到成功或用完
     */
    @PostMapping("/full-pipeline")
    public ApiResult<PipelineResponse> fullPipeline(@RequestBody PipelineRequest req) {
        log.info("[DEBUG] 流水线测试 | 共{}个SQL | 数据源: {}", req.getSqlSequence().size(), req.getDataSourceId());

        DatabaseSchema schema = schemaExtractService.extractSchema(req.getDataSourceId());
        PipelineResponse resp = new PipelineResponse();

        for (int i = 0; i < req.getSqlSequence().size(); i++) {
            String sql = req.getSqlSequence().get(i);

            // --- 静态校验 ---
            StepResult step1 = new StepResult();
            step1.setAttempt(i);
            step1.setSql(sql);
            step1.setStage("STATIC_VALIDATE");

            SqlValidateService.ValidationResult vr = sqlValidateService.validate(sql, schema);
            if (!vr.isValid()) {
                step1.setPassed(false);
                step1.setErrorType("SYNTAX_ERROR");
                step1.setErrorMessage(vr.getErrorMessage());
                resp.getSteps().add(step1);
                log.info("[DEBUG] 第{}轮 静态校验失败: {}", i + 1, vr.getErrorMessage());
                continue; // 取下一个SQL
            }
            step1.setPassed(true);
            resp.getSteps().add(step1);

            // --- 动态执行 ---
            StepResult step2 = new StepResult();
            step2.setAttempt(i);
            step2.setSql(sql);
            step2.setStage("EXECUTE");

            try {
                QueryResult qr = sqlExecuteService.execute(req.getDataSourceId(), sql);
                step2.setPassed(true);
                resp.getSteps().add(step2);
                resp.setFinalSuccess(true);
                resp.setFinalSql(sql);
                resp.setQueryResult(qr);
                resp.setTotalRetries(i);
                log.info("[DEBUG] 第{}轮 执行成功！共重试{}次", i + 1, i);
                return ApiResult.success(resp);
            } catch (Exception e) {
                step2.setPassed(false);
                step2.setErrorType("EXECUTION_ERROR");
                step2.setErrorMessage(e.getMessage());
                resp.getSteps().add(step2);
                log.info("[DEBUG] 第{}轮 执行失败: {}", i + 1, e.getMessage());
                // 继续取下一个SQL
            }
        }

        // 所有SQL都失败了
        resp.setFinalSuccess(false);
        resp.setTotalRetries(req.getSqlSequence().size());
        log.info("[DEBUG] 所有{}个SQL都失败了", req.getSqlSequence().size());
        return ApiResult.success(resp);
    }

    /**
     * 查看提取的Schema DDL
     */
    @GetMapping("/schema/{dsId}")
    public ApiResult<String> getSchema(@PathVariable String dsId) {
        DatabaseSchema schema = schemaExtractService.extractSchema(dsId);
        return ApiResult.success(schema.formatAsDDL());
    }
}
