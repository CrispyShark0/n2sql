package com.itheima.n2sql.model.enums;

/**
 * 查询意图类型枚举
 *
 * 根据用户的自然语言问题，将查询分为不同类型。
 * 不同类型使用不同的提示词模板，提升 SQL 生成准确率。
 *
 * 分类依据（关键词匹配）：
 *   SIMPLE     — 简单查询（查找、列出、查看）
 *   AGGREGATE  — 聚合统计（总数、平均、最大、最小、求和、统计、占比）
 *   MULTI_JOIN — 多表联查（问题涉及多个实体的关联，如"用户的订单"）
 *   NESTED     — 嵌套/高级查询（排名、Top N by group、环比、子查询）
 */
public enum QueryType {

    /**
     * 简单查询：单表、无聚合、无JOIN
     * 例如："查询所有用户"、"找出年龄大于25的员工"
     */
    SIMPLE("simple_nl2sql", "简单查询"),

    /**
     * 聚合统计：涉及 COUNT/SUM/AVG/MAX/MIN/GROUP BY
     * 例如："统计每个部门的人数"、"计算平均薪资"
     */
    AGGREGATE("aggregate_nl2sql", "聚合统计"),

    /**
     * 多表联查：涉及 JOIN 操作
     * 例如："查询用户及其订单信息"、"列出每个客户购买的产品"
     */
    MULTI_JOIN("multijoin_nl2sql", "多表联查"),

    /**
     * 嵌套/高级查询：子查询、窗口函数、排名等
     * 例如："每个部门薪资最高的员工"、"销售额排名前3的产品类别"
     */
    NESTED("nested_nl2sql", "嵌套/高级查询");

    /** 对应的提示词模板名称（在 PromptTemplateService 中注册） */
    private final String templateName;

    /** 中文描述（用于日志和调试） */
    private final String description;

    QueryType(String templateName, String description) {
        this.templateName = templateName;
        this.description = description;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getDescription() {
        return description;
    }
}
