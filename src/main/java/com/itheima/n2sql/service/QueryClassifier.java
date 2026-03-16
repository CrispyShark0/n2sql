package com.itheima.n2sql.service;

import com.itheima.n2sql.model.dto.DatabaseSchema;
import com.itheima.n2sql.model.enums.QueryType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 查询意图分类器
 *
 * 根据用户的自然语言问题，判断属于哪种查询类型（简单/聚合/多表/嵌套）。
 * 使用关键词匹配 + Schema 信息辅助判断。
 *
 * 分类优先级（从高到低）：
 *   NESTED     — 最复杂，优先检测
 *   AGGREGATE  — 涉及统计
 *   MULTI_JOIN — 涉及多表
 *   SIMPLE     — 兜底，以上都不是就是简单查询
 *
 * 为什么不用大模型来分类？
 *   1. 速度快：关键词匹配是毫秒级，大模型分类要几秒
 *   2. 省钱：不消耗额外的 API 调用
 *   3. 可控：规则明确，不会出现"大模型分错类"的情况
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

    /** 多表联查的"关联信号"词 — 问题中同时提到两种不同的实体 */
    private static final List<String> JOIN_SIGNAL_KEYWORDS = List.of(
            "的订单", "的产品", "的用户", "的客户", "的员工",
            "购买了", "下单了", "属于", "关联",
            "及其", "以及", "和他们的", "和它们的",
            "包含", "对应的", "所属的",
            "哪些人", "哪些用户", "哪些客户"
    );

    /**
     * 对用户问题进行意图分类
     *
     * @param question 用户的自然语言问题
     * @param schema   数据库结构（辅助判断是否涉及多表）
     * @return 查询类型枚举
     */
    public QueryType classify(String question, DatabaseSchema schema) {
        if (question == null || question.isBlank()) {
            return QueryType.SIMPLE;
        }

        String q = question.toLowerCase();

        // 优先级1：检测嵌套/高级查询
        if (matchesAny(q, NESTED_KEYWORDS)) {
            log.info("查询分类: NESTED（嵌套/高级） | 问题: {}", question);
            return QueryType.NESTED;
        }

        // 优先级2：检测聚合统计
        boolean isAggregate = matchesAny(q, AGGREGATE_KEYWORDS);

        // 优先级3：检测多表联查
        boolean isMultiJoin = matchesAny(q, JOIN_SIGNAL_KEYWORDS)
                || mentionsMultipleTables(q, schema);

        // 组合判断
        if (isAggregate && isMultiJoin) {
            // 既有聚合又有多表 → 归为嵌套（最复杂的模板）
            log.info("查询分类: NESTED（聚合+多表） | 问题: {}", question);
            return QueryType.NESTED;
        }

        if (isAggregate) {
            log.info("查询分类: AGGREGATE（聚合统计） | 问题: {}", question);
            return QueryType.AGGREGATE;
        }

        if (isMultiJoin) {
            log.info("查询分类: MULTI_JOIN（多表联查） | 问题: {}", question);
            return QueryType.MULTI_JOIN;
        }

        // 兜底：简单查询
        log.info("查询分类: SIMPLE（简单查询） | 问题: {}", question);
        return QueryType.SIMPLE;
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
     *
     * 例如数据库有 users（用户表）和 orders（订单表）
     * 用户问 "查询用户的订单"，同时提到了"用户"和"订单"，说明涉及多表
     */
    private boolean mentionsMultipleTables(String question, DatabaseSchema schema) {
        if (schema == null || schema.getTables() == null) {
            return false;
        }

        int matchedTableCount = 0;
        for (var table : schema.getTables()) {
            String tableName = table.getTableName().toLowerCase();
            String tableComment = table.getTableComment();

            // 检查问题中是否提到了这张表（表名或表注释）
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
