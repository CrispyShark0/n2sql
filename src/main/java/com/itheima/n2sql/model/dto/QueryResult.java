package com.itheima.n2sql.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQL 查询结果 DTO
 *
 * 存储 SQL 执行后的结构化结果。
 * 类似于你在 Navicat/DataGrip 里看到的查询结果表格。
 *
 * 举例：SELECT name, age FROM users 的结果：
 *   columns = ["name", "age"]
 *   rows = [{"name":"张三","age":25}, {"name":"李四","age":30}]
 *   rowCount = 2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResult {

    /** 列名列表，如 ["name", "age", "email"] */
    @Builder.Default
    private List<String> columns = new ArrayList<>();

    /**
     * 数据行列表
     * 每一行是一个 Map：Key=列名，Value=该列的值
     * 如 [{"name":"张三","age":25}, {"name":"李四","age":30}]
     */
    @Builder.Default
    private List<Map<String, Object>> rows = new ArrayList<>();

    /** 结果总行数 */
    private int rowCount;

    /** SQL 执行耗时（毫秒） */
    private long executeTimeMs;
}
