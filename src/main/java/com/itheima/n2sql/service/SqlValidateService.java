package com.itheima.n2sql.service;

import com.itheima.n2sql.model.dto.DatabaseSchema;
import com.itheima.n2sql.model.dto.TableSchema;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SQL 静态验证服务（两级验证）
 *
 * 第一级：语法验证（JSQLParser 解析）
 *   - 检查 SQL 语法是否合法（括号匹配、关键字拼写等）
 *   - 检查是否是 SELECT 语句
 *
 * 第二级：Schema 校验（表名 + 列名比对）
 *   - 检查 SQL 中用到的表名是否存在于数据库 Schema 中
 *   - 检查 SQL 中明确写出的列名是否存在于对应的表中
 *   - 不需要连接数据库，用已扫描的 DatabaseSchema 对象做本地比对
 *
 * 两级都通过后，才会进入数据库执行阶段。
 * 这样可以快速过滤掉明显的错误，减少无效的数据库查询。
 */
@Slf4j
@Service
public class SqlValidateService {

    /**
     * 完整验证：语法 + Schema 校验
     *
     * @param sql    待验证的 SQL 语句
     * @param schema 当前数据库的结构信息（由 SchemaExtractService 扫描得到）
     * @return ValidationResult 验证结果
     */
    public ValidationResult validate(String sql, DatabaseSchema schema) {
        // === 第一级：语法验证 ===
        if (sql == null || sql.isBlank()) {
            return ValidationResult.fail("SQL 语句为空");
        }

        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(sql);
        } catch (Exception e) {
            String errorMsg = extractErrorMessage(e);
            log.warn("语法验证失败: {} | SQL: {}", errorMsg, sql);
            return ValidationResult.fail("SQL 语法错误: " + errorMsg);
        }

        if (!(statement instanceof Select)) {
            return ValidationResult.fail("不是 SELECT 查询语句，系统只允许执行查询操作");
        }

        log.debug("第一级语法验证通过");

        // === 第二级：Schema 校验（表名 + 列名） ===
        if (schema != null) {
            ValidationResult schemaResult = validateSchema(statement, schema);
            if (!schemaResult.isValid()) {
                return schemaResult;
            }
            log.debug("第二级 Schema 校验通过");
        }

        log.debug("SQL 静态验证全部通过: {}", sql);
        return ValidationResult.ok();
    }

    /**
     * Schema 校验：检查表名和列名是否存在
     *
     * @param statement 已解析的 SQL 语法树
     * @param schema    数据库结构
     * @return 验证结果
     */
    private ValidationResult validateSchema(Statement statement, DatabaseSchema schema) {
        // --- 构建 Schema 查询字典（方便快速查找） ---
        // allTableNames: 数据库中所有表名的集合（全小写，忽略大小写差异）
        Set<String> allTableNames = schema.getTables().stream()
                .map(t -> t.getTableName().toLowerCase())
                .collect(Collectors.toSet());

        // tableColumnMap: 每张表有哪些列  Key=表名(小写), Value=列名集合(小写)
        Map<String, Set<String>> tableColumnMap = new HashMap<>();
        for (TableSchema table : schema.getTables()) {
            Set<String> colNames = table.getColumns().stream()
                    .map(c -> c.getColumnName().toLowerCase())
                    .collect(Collectors.toSet());
            tableColumnMap.put(table.getTableName().toLowerCase(), colNames);
        }

        // --- 第一步：校验表名 ---
        // TablesNamesFinder 是 JSQLParser 自带的工具，能从 SQL 中提取所有表名
        // 它能自动处理别名（如 users u → 提取 users）、JOIN、子查询等
        TablesNamesFinder tablesFinder = new TablesNamesFinder();
        List<String> sqlTableNames = tablesFinder.getTableList(statement);

        for (String tableName : sqlTableNames) {
            if (!allTableNames.contains(tableName.toLowerCase())) {
                // 找出最相似的表名，给出修正建议
                String suggestion = findSimilar(tableName, allTableNames);
                String errorMsg = "表 '" + tableName + "' 不存在于数据库中。"
                        + "数据库中的表有: " + allTableNames
                        + (suggestion != null ? "。你是不是想用: " + suggestion : "");
                log.warn("Schema 校验失败（表名）: {}", errorMsg);
                return ValidationResult.fail(errorMsg);
            }
        }

        // --- 第二步：校验列名 ---
        // 构建 SQL 中的别名映射：别名 → 真实表名
        // 例如 "SELECT u.name FROM users u" 中 u → users
        Map<String, String> aliasToTable = buildAliasMap(statement);

        // 从 SQL 中提取所有列引用
        List<Column> columns = extractColumns(statement);

        // 所有表的所有列合集（用于无法确定表归属时的模糊校验）
        Set<String> allColumnNames = tableColumnMap.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        for (Column column : columns) {
            String colName = column.getColumnName().toLowerCase();

            // 跳过通配符 *
            if ("*".equals(column.getColumnName())) {
                continue;
            }

            // 判断列属于哪张表
            Table colTable = column.getTable();
            if (colTable != null && colTable.getName() != null) {
                // 有表前缀，如 u.name 或 users.name
                String tableRef = colTable.getName().toLowerCase();
                // 如果是别名，转换为真实表名
                String realTable = aliasToTable.getOrDefault(tableRef, tableRef);

                Set<String> tableCols = tableColumnMap.get(realTable);
                if (tableCols != null && !tableCols.contains(colName)) {
                    String suggestion = findSimilar(colName, tableCols);
                    String errorMsg = "列 '" + column.getColumnName()
                            + "' 不存在于表 '" + realTable + "' 中。"
                            + "该表的列有: " + tableCols
                            + (suggestion != null ? "。你是不是想用: " + suggestion : "");
                    log.warn("Schema 校验失败（列名）: {}", errorMsg);
                    return ValidationResult.fail(errorMsg);
                }
            } else {
                // 没有表前缀（如 SELECT name FROM users）
                // 在所有表的列中模糊查找，找不到才报错
                if (!allColumnNames.contains(colName)) {
                    String suggestion = findSimilar(colName, allColumnNames);
                    String errorMsg = "列 '" + column.getColumnName()
                            + "' 不存在于数据库的任何表中"
                            + (suggestion != null ? "。你是不是想用: " + suggestion : "");
                    log.warn("Schema 校验失败（列名）: {}", errorMsg);
                    return ValidationResult.fail(errorMsg);
                }
            }
        }

        return ValidationResult.ok();
    }

    /**
     * 构建别名映射：从 SQL 的 FROM/JOIN 子句中提取 表别名 → 真实表名
     *
     * 例如 "SELECT u.name FROM users u JOIN orders o ON ..."
     * 结果：{"u" → "users", "o" → "orders"}
     */
    private Map<String, String> buildAliasMap(Statement statement) {
        Map<String, String> aliasMap = new HashMap<>();
        if (!(statement instanceof PlainSelect plainSelect)) {
            return aliasMap;
        }

        // 从 FROM 子句提取
        FromItem fromItem = plainSelect.getFromItem();
        extractAliasFromItem(fromItem, aliasMap);

        // 从 JOIN 子句提取
        List<Join> joins = plainSelect.getJoins();
        if (joins != null) {
            for (Join join : joins) {
                extractAliasFromItem(join.getRightItem(), aliasMap);
            }
        }

        return aliasMap;
    }

    /**
     * 从一个 FROM 项中提取别名
     */
    private void extractAliasFromItem(FromItem fromItem, Map<String, String> aliasMap) {
        if (fromItem instanceof Table table) {
            if (table.getAlias() != null) {
                String alias = table.getAlias().getName().toLowerCase();
                String realName = table.getName().toLowerCase();
                aliasMap.put(alias, realName);
            }
        }
    }

    /**
     * 从 SQL 语法树中提取所有列引用（Column 对象）
     *
     * 只提取直接的列引用，跳过函数内部的参数（如 COUNT(id) 中的 id 不提取，
     * 因为聚合函数参数的校验比较复杂，交给数据库执行阶段处理）
     */
    private List<Column> extractColumns(Statement statement) {
        List<Column> columns = new ArrayList<>();
        if (!(statement instanceof PlainSelect plainSelect)) {
            return columns;
        }

        // 从 SELECT 列表提取
        for (SelectItem<?> item : plainSelect.getSelectItems()) {
            Expression expr = item.getExpression();
            if (expr != null) {
                extractColumnsFromExpression(expr, columns);
            }
        }

        // 从 WHERE 子句提取
        if (plainSelect.getWhere() != null) {
            extractColumnsFromExpression(plainSelect.getWhere(), columns);
        }

        return columns;
    }

    /**
     * 递归地从表达式中提取 Column 对象
     * 跳过函数参数（如 COUNT(id)、SUM(amount)），这些交给数据库校验
     */
    private void extractColumnsFromExpression(Expression expr, List<Column> columns) {
        if (expr instanceof Column col) {
            columns.add(col);
        }
        // 不递归进入 Function 内部，避免误报
        // 例如 COUNT(id) 中的 id 可能在某些数据库方言中有特殊处理
    }

    /**
     * 从候选集中找到和目标最相似的字符串（简单的编辑距离匹配）
     * 用于给出 "你是不是想用 xxx" 的修正建议
     *
     * @param target     目标字符串
     * @param candidates 候选集
     * @return 最相似的候选，如果没有足够相似的返回 null
     */
    private String findSimilar(String target, Set<String> candidates) {
        String targetLower = target.toLowerCase();
        String best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (String candidate : candidates) {
            int dist = editDistance(targetLower, candidate.toLowerCase());
            if (dist < bestDistance) {
                bestDistance = dist;
                best = candidate;
            }
        }

        // 只有编辑距离 <= 3 才认为足够相似，值得推荐
        return (best != null && bestDistance <= 3) ? best : null;
    }

    /**
     * 计算两个字符串的编辑距离（Levenshtein Distance）
     * 编辑距离 = 把字符串A变成字符串B最少需要几步操作（插入/删除/替换）
     * 例如：user → users 编辑距离=1，form → from 编辑距离=2
     */
    private int editDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    /**
     * 从 JSQLParser 的异常中提取有用的错误信息
     */
    private String extractErrorMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            return "未知语法错误";
        }
        if (msg.length() > 200) {
            msg = msg.substring(0, 200) + "...";
        }
        return msg;
    }

    // ========== 内部类：验证结果 ==========

    public static class ValidationResult {

        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult fail(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
