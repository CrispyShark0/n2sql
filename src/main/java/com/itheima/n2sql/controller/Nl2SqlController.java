package com.itheima.n2sql.controller;

import com.itheima.n2sql.model.dto.ApiResult;
import com.itheima.n2sql.model.dto.Nl2SqlRequest;
import com.itheima.n2sql.model.dto.Nl2SqlResponse;
import com.itheima.n2sql.service.Nl2SqlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * NL2SQL 核心接口 Controller
 *
 * 提供自然语言转 SQL 的 HTTP 接口。
 * 这是整个系统最核心的接口——前端传一个中文问题，后端返回 SQL 和查询结果。
 *
 * 接口列表：
 *   POST /api/nl2sql — 自然语言转SQL并执行
 */
@Slf4j
@RestController
@RequestMapping("/api/nl2sql")
@RequiredArgsConstructor
public class Nl2SqlController {

    /** NL2SQL 核心服务（Spring 自动注入） */
    private final Nl2SqlService nl2SqlService;

    /**
     * 自然语言转 SQL 并执行
     *
     * POST /api/nl2sql
     * 请求体示例（JSON）：
     * {
     *   "dataSourceId": "ds-a1b2c3d4",
     *   "question": "查询销售额最高的前5个产品"
     * }
     *
     * 返回值包含：
     *   - 生成的 SQL 语句
     *   - SQL 执行结果（列名 + 数据行）
     *   - 是否成功
     *   - 纠错历史（如果触发了自纠错）
     *
     * @param request 包含数据源ID和自然语言问题
     * @return NL2SQL 完整结果
     */
    @PostMapping
    public ApiResult<Nl2SqlResponse> generateAndExecute(
            @Valid @RequestBody Nl2SqlRequest request) {
        log.info("NL2SQL 请求 | 数据源: {} | 问题: {}",
                request.getDataSourceId(), request.getQuestion());

        Nl2SqlResponse response = nl2SqlService.generateSql(
                request.getDataSourceId(), request.getQuestion());

        if (response.isSuccess()) {
            log.info("NL2SQL 成功 | SQL: {}", response.getGeneratedSql());
            return ApiResult.success(response);
        } else {
            log.warn("NL2SQL 失败 | 错误: {}", response.getErrorMessage());
            // 即使失败也返回 200 状态码，但 response 中的 success=false
            // 前端根据 response.success 判断，而不是 HTTP 状态码
            return ApiResult.success(response);
        }
    }
}
