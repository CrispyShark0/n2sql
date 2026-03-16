package com.itheima.n2sql.service;

import com.itheima.n2sql.config.DataSourceManager;
import com.itheima.n2sql.exception.BizException;
import com.itheima.n2sql.model.dto.QueryResult;
import com.itheima.n2sql.util.SqlCleanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL 执行引擎
 *
 * 负责在指定数据源上安全地执行 SQL 查询，并将结果转成结构化的 QueryResult。
 *
 * 安全措施：
 *   1. 只允许 SELECT 语句（禁止 DELETE/UPDATE/DROP 等）
 *   2. 查询超时限制（默认 30 秒）
 *   3. 返回行数限制（默认 1000 行）
 *
 * @Value 注解：从 yml 配置文件中读取单个配置值
 *   比如 @Value("${n2sql.sql.timeout-seconds:30}") 意思是：
 *   去 yml 里找 n2sql.sql.timeout-seconds，找不到就用默认值 30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlExecuteService {

    /** 数据源管理器 */
    private final DataSourceManager dataSourceManager;

    /** 查询超时时间（秒），从 yml 配置读取，默认 30 秒 */
    @Value("${n2sql.sql.timeout-seconds:30}")
    private int timeoutSeconds;

    /** 最大返回行数，从 yml 配置读取，默认 1000 行 */
    @Value("${n2sql.sql.max-rows:1000}")
    private int maxRows;

    /**
     * 在指定数据源上执行 SQL 查询
     *
     * @param dataSourceId 数据源ID
     * @param sql          要执行的 SQL 语句
     * @return QueryResult 查询结果（列名 + 数据行 + 耗时）
     */
    public QueryResult execute(String dataSourceId, String sql) {
        // ① 安全检查：只允许 SELECT 语句
        if (!SqlCleanUtil.isSelectStatement(sql)) {
            throw new BizException("安全限制：只允许执行 SELECT 查询语句");
        }

        log.info("执行 SQL | 数据源: {} | SQL: {}", dataSourceId, sql);
        long startTime = System.currentTimeMillis();

        // try-with-resources：自动关闭 Connection 和 Statement
        try (Connection conn = dataSourceManager.getConnection(dataSourceId);
             Statement stmt = conn.createStatement()) {

            // ② 设置安全限制
            stmt.setQueryTimeout(timeoutSeconds);  // 超时限制
            stmt.setMaxRows(maxRows);              // 行数限制

            // ③ 执行查询
            ResultSet rs = stmt.executeQuery(sql);

            // ④ 把 ResultSet 转成 QueryResult
            QueryResult result = convertResultSet(rs);

            long costTime = System.currentTimeMillis() - startTime;
            result.setExecuteTimeMs(costTime);

            log.info("SQL 执行成功 | 返回 {} 行 | 耗时 {}ms", result.getRowCount(), costTime);
            return result;

        } catch (SQLException e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("SQL 执行失败 ({}ms): {}", costTime, e.getMessage());
            throw new BizException("SQL 执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 JDBC 的 ResultSet 转换为我们的 QueryResult 对象
     *
     * ResultSet 是 JDBC 返回的"游标"，需要一行一行读取。
     * 我们把它转成 List<Map> 结构，方便 JSON 序列化返回给前端。
     *
     * @param rs JDBC 查询结果集
     * @return QueryResult 结构化结果
     */
    private QueryResult convertResultSet(ResultSet rs) throws SQLException {
        // 获取结果集的元信息（有几列、每列叫什么名字）
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // 提取列名列表
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            // getColumnLabel 优先返回别名（如 SELECT name AS 姓名），没有别名则返回列名
            columns.add(metaData.getColumnLabel(i));
        }

        // 逐行读取数据
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            // 每一行用 LinkedHashMap 存储（LinkedHashMap 保持插入顺序，列的顺序不会乱）
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(columns.get(i - 1), rs.getObject(i));
            }
            rows.add(row);
        }

        return QueryResult.builder()
                .columns(columns)
                .rows(rows)
                .rowCount(rows.size())
                .build();
    }
}
