package com.itheima.n2sql.controller;

import com.itheima.n2sql.model.dto.ApiResult;
import com.itheima.n2sql.model.dto.DataSourceCreateRequest;
import com.itheima.n2sql.model.dto.DataSourceTestResponse;
import com.itheima.n2sql.model.entity.DataSourceInfo;
import com.itheima.n2sql.service.DataSourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据源管理 Controller
 *
 * 提供数据源的增删查 + 测试连接的 HTTP 接口。
 * 所有接口路径都以 /api/datasource 开头。
 *
 * 接口列表：
 *   POST   /api/datasource          — 创建数据源
 *   GET    /api/datasource          — 获取所有数据源列表
 *   GET    /api/datasource/{id}     — 根据ID获取单个数据源
 *   DELETE /api/datasource/{id}     — 删除数据源
 *   POST   /api/datasource/test     — 测试数据源连接
 */
@Slf4j
@RestController
@RequestMapping("/api/datasource")
@RequiredArgsConstructor
public class DataSourceController {

    /** 数据源业务服务（Spring 自动注入） */
    private final DataSourceService dataSourceService;

    /**
     * 创建数据源
     *
     * POST /api/datasource
     * 请求体示例（JSON）：
     * {
     *   "name": "测试数据库",
     *   "dbType": "MYSQL",
     *   "host": "localhost",
     *   "port": 3306,
     *   "dbName": "test_db",
     *   "username": "root",
     *   "password": "123456"
     * }
     *
     * @param request 创建请求（@Valid 触发参数校验，@RequestBody 把 JSON 转成 Java 对象）
     * @return 创建好的数据源信息（包含系统生成的ID）
     */
    @PostMapping
    public ApiResult<DataSourceInfo> create(@Valid @RequestBody DataSourceCreateRequest request) {
        log.info("创建数据源: {}", request.getName());
        DataSourceInfo info = dataSourceService.create(request);
        return ApiResult.success(info);
    }

    /**
     * 获取所有数据源列表
     *
     * GET /api/datasource
     *
     * @return 数据源列表
     */
    @GetMapping
    public ApiResult<List<DataSourceInfo>> listAll() {
        List<DataSourceInfo> list = dataSourceService.listAll();
        return ApiResult.success(list);
    }

    /**
     * 根据ID获取单个数据源
     *
     * GET /api/datasource/{id}
     * 例如：GET /api/datasource/ds-a1b2c3d4
     *
     * @PathVariable 从 URL 路径中提取 {id} 的值
     */
    @GetMapping("/{id}")
    public ApiResult<DataSourceInfo> getById(@PathVariable String id) {
        DataSourceInfo info = dataSourceService.getById(id);
        return ApiResult.success(info);
    }

    /**
     * 删除数据源
     *
     * DELETE /api/datasource/{id}
     * 例如：DELETE /api/datasource/ds-a1b2c3d4
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable String id) {
        log.info("删除数据源: {}", id);
        dataSourceService.delete(id);
        return ApiResult.success();
    }

    /**
     * 测试数据源连接
     *
     * POST /api/datasource/test
     * 请求体和创建数据源一样的格式，但不会真正保存，只是测试能不能连上。
     *
     * @param request 数据源信息
     * @return 测试结果（成功/失败 + 数据库版本 + 耗时）
     */
    @PostMapping("/test")
    public ApiResult<DataSourceTestResponse> testConnection(
            @Valid @RequestBody DataSourceCreateRequest request) {
        log.info("测试数据源连接: {} ({})", request.getName(), request.getDbType());
        DataSourceTestResponse result = dataSourceService.testConnection(request);
        return ApiResult.success(result);
    }
}
