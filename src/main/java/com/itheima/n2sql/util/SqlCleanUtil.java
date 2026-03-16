package com.itheima.n2sql.util;

/**
 * SQL 清洗工具类
 *
 * 大模型返回的文本可能包含 Markdown 格式（```sql ... ```）、
 * 多余的空行、分号等，这个工具负责提取出干净的 SQL 语句。
 *
 * 为什么需要这个？举例：
 *   大模型可能返回：
 *     "以下是查询语句：\n```sql\nSELECT * FROM users;\n```\n希望对你有帮助"
 *   我们只需要：
 *     "SELECT * FROM users"
 */
public class SqlCleanUtil {

    /**
     * 从大模型返回的文本中提取干净的 SQL
     *
     * 处理以下情况：
     *   1. 被 ```sql ... ``` 包裹的代码块
     *   2. 被 ``` ... ``` 包裹的代码块（没有 sql 标识）
     *   3. 前后的多余空白
     *   4. 末尾的分号（执行时某些场景不需要）
     *   5. 前后的多余文字解释
     *
     * @param rawText 大模型返回的原始文本
     * @return 干净的 SQL 语句
     */
    public static String cleanSql(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String sql = rawText.trim();

        // 情况1：提取 ```sql ... ``` 代码块中的内容
        if (sql.contains("```sql")) {
            int start = sql.indexOf("```sql") + 6;  // 跳过 "```sql" 这6个字符
            int end = sql.indexOf("```", start);     // 找到结束的 ```
            if (end > start) {
                sql = sql.substring(start, end);
            }
        }
        // 情况2：提取 ``` ... ``` 代码块中的内容
        else if (sql.contains("```")) {
            int start = sql.indexOf("```") + 3;
            int end = sql.indexOf("```", start);
            if (end > start) {
                sql = sql.substring(start, end);
            }
        }

        // 去除前后空白
        sql = sql.trim();

        // 去除末尾分号（JDBC 执行时不需要分号）
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }

        return sql;
    }

    /**
     * 简单判断一段文本是否像一个 SELECT 查询语句
     *
     * @param sql 待检查的文本
     * @return true 如果以 SELECT 开头（忽略大小写）
     */
    public static boolean isSelectStatement(String sql) {
        if (sql == null || sql.isBlank()) {
            return false;
        }
        return sql.trim().toUpperCase().startsWith("SELECT");
    }
}
