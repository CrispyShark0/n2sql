package com.itheima.n2sql.service;

import com.itheima.n2sql.model.dto.DatabaseSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 查询意图检测器（软策略版本）
 *
 * 【改进说明 — 按老师要求】
 * 旧方案：把查询硬分类为 SIMPLE/AGGREGATE/MULTI_JOIN/NESTED 四类，各用不同模板。
 *   缺点：一旦分错类，使用了错误的模板，反而比不分类更差。
 *
 * 新方案（软策略）：
 *   1. 始终使用一个通用的基础 prompt 模板兜底
 *   2. 根据检测到的关键词，动态追加补充指令（hints）
 *   3. 多种 hints 可以同时追加，不互斥
 *
 * 例如用户问 "查询每个部门销售额最高的员工"：
 *   → 检测到 "每个"      → 追加聚合提示
 *   → 检测到 "最高"      → 追加聚合提示（已追加则跳过）
 *   → 检测到 "部门""员工" → 追加多表联查提示
 *   → 检测到 "每个.*最"   → 追加嵌套/窗口函数提示
 *   最终 prompt = 基础模板 + 聚合提示 + 多表提示 + 嵌套提示
 *
 * 好处：不会因为"分错类"而用错模板，多个提示叠加让大模型获得更全面的指导。
 */
@Slf4j
@Service
public class QueryClassifier {

    // ========== 关键词列表（用于模式匹配） ==========

    /** 嵌套/高级查询关键词 */
    private static final List<String> NESTED_KEYWORDS = List.of(
            "排名", "排行", "前几", "前\\d+", "top\\s*\\d+",
            "每个.*最", "各.*最", "每.*排名",
            "子查询", "嵌套",
            "环比", "同比", "增长率", "变化率",
            "占比", "百分比", "比例",
            "高于平均", "低于平均", "超过平均",
            "不在", "不包含", "除了.*之外"
    );

    /** 聚合统计关键词 */
    private static final List<String> AGGREGATE_KEYWORDS = List.of(
            "多少", "几个", "数量", "总数", "计数", "统计",
            "总计", "合计", "总额", "总和", "求和",
            "平均", "均值", "平均值",
            "最大", "最高", "最多", "最贵",
            "最小", "最低", "最少", "最便宜",
            "分组", "按.*分", "各个", "每个", "分别",
            "汇总", "分布"
    );

    /** 多表联查的"关联信号"词 */
    private static final List<String> JOIN_SIGNAL_KEYWORDS = List.of(
            "的订单", "的产品", "的用户", "的客户", "的员工",
            "购买了", "下单了", "属于", "关联",
            "及其", "以及", "和他们的", "和它们的",
            "包含", "对应的", "所属的",
            "哪些人", "哪些用户", "哪些客户"
    );

    // ========== 补充指令文本（追加到基础 prompt 后面） ==========

    /** 聚合统计补充指令 */
    private static final String AGGREGATE_HINT = """
            
            ## Additional Hint: Aggregation Detected
            The user's question involves AGGREGATION or STATISTICS. Please pay special attention to:
            - Use appropriate aggregate functions: COUNT(*), SUM(), AVG(), MAX(), MIN()
            - Always include GROUP BY clause when using aggregate functions with non-aggregated columns
            - Use HAVING clause (not WHERE) to filter on aggregated results
            - Use ORDER BY on the aggregated column to show results in a meaningful order
            - Use LIMIT when the user asks for "top N" or "bottom N" results
            - Give meaningful aliases to aggregated columns (e.g., SUM(amount) AS total_amount)
            """;

    /** 多表联查补充指令 */
    private static final String MULTI_JOIN_HINT = """
            
            ## Additional Hint: Multi-Table JOIN Detected
            The user's question involves MULTIPLE TABLES. Please pay special attention to:
            - Use proper JOIN operations (INNER JOIN / LEFT JOIN) based on the query intent
            - ALWAYS use the foreign key relationships shown in the schema comments for JOIN conditions
            - Use table aliases for readability (e.g., employees e, departments d)
            - Qualify column names with table aliases to avoid ambiguity (e.g., e.emp_name, d.dept_name)
            - Select meaningful columns from each joined table, not just SELECT *
            """;

    /** 嵌套/高级查询补充指令 */
    private static final String NESTED_HINT = """
            
            ## Additional Hint: Complex/Nested Query Detected
            The user's question involves COMPLEX LOGIC. Please choose the best strategy:
            - For "Top N per group" (e.g., "每个部门薪资最高的员工"): Use window functions like ROW_NUMBER() OVER (PARTITION BY ... ORDER BY ... DESC), wrap in subquery and filter WHERE rn <= N
            - For comparisons against aggregates (e.g., "高于平均薪资"): Use subqueries like WHERE salary > (SELECT AVG(salary) FROM ...)
            - For "NOT IN" / exclusion queries: Use NOT EXISTS or LEFT JOIN ... WHERE ... IS NULL
            - For ranking queries: Use RANK() or DENSE_RANK() window functions
            - For ratio/percentage queries: Use column_value * 100.0 / SUM(column_value) OVER () AS percentage
            - Use proper table aliases and qualify ALL column names to avoid ambiguity
            """;

    /**
     * 【核心方法】检测用户问题中的关键词，返回需要追加的补充指令列表
     *
     * 软策略：不做互斥分类，多种提示可以同时返回。
     *
     * @param question 用户的自然语言问题
     * @param schema   数据库结构（辅助判断是否涉及多表）
     * @return 补充指令列表（可能为空，表示简单查询不需要额外提示）
     */
    public List<String> detectHints(String question, DatabaseSchema schema) {
        List<String> hints = new ArrayList<>();

        if (question == null || question.isBlank()) {
            return hints;
        }

        String q = question.toLowerCase();

        // 检测聚合统计
        if (matchesAny(q, AGGREGATE_KEYWORDS)) {
            hints.add(AGGREGATE_HINT);
            log.info("检测到聚合关键词 | 问题: {}", question);
        }

        // 检测多表联查
        if (matchesAny(q, JOIN_SIGNAL_KEYWORDS) || mentionsMultipleTables(q, schema)) {
            hints.add(MULTI_JOIN_HINT);
            log.info("检测到多表联查信号 | 问题: {}", question);
        }

        // 检测嵌套/高级查询
        if (matchesAny(q, NESTED_KEYWORDS)) {
            hints.add(NESTED_HINT);
            log.info("检测到嵌套/高级查询关键词 | 问题: {}", question);
        }

        if (hints.isEmpty()) {
            log.info("简单查询（无额外提示） | 问题: {}", question);
        } else {
            log.info("共检测到 {} 种查询提示 | 问题: {}", hints.size(), question);
        }

        return hints;
    }

    /**
     * 检查问题是否匹配关键词列表中的任意一个
     * 支持正则表达式（如 "前\\d+" 可以匹配 "前5"、"前10"）
     */
    private boolean matchesAny(String question, List<String> keywords) {
        for (String keyword : keywords) {
            try {
                if (Pattern.compile(keyword).matcher(question).find()) {
                    return true;
                }
            } catch (Exception e) {
                // 如果正则语法有误，回退到普通包含匹配
                if (question.contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查问题是否提到了多张表（通过表名或表注释匹配）
     */
    private boolean mentionsMultipleTables(String question, DatabaseSchema schema) {
        if (schema == null || schema.getTables() == null) {
            return false;
        }

        int matchedTableCount = 0;
        for (var table : schema.getTables()) {
            String tableName = table.getTableName().toLowerCase();
            String tableComment = table.getTableComment();

            boolean mentioned = question.contains(tableName);
            if (!mentioned && tableComment != null && !tableComment.isEmpty()) {
                mentioned = question.contains(tableComment.toLowerCase());
            }

            if (mentioned) {
                matchedTableCount++;
            }
        }

        return matchedTableCount >= 2;
    }
}
