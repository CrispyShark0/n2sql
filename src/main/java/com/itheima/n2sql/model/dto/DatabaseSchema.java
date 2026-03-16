package com.itheima.n2sql.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库Schema DTO
 *
 * 描述一整个数据库的结构：包含多张表的信息。
 * 最重要的方法是 formatAsDDL()，它把所有表结构转成 CREATE TABLE 语句的文本，
 * 后续会塞进提示词里，让大模型"看懂"数据库结构。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseSchema {

    /** 数据库名称 */
    private String databaseName;

    /** 数据库类型（如 "MYSQL"、"POSTGRESQL"），第五阶段新增 */
    private String dbType;

    /** 所有表的结构信息 */
    @Builder.Default
    private List<TableSchema> tables = new ArrayList<>();

    /**
     * 把整个数据库结构格式化为 DDL 文本（CREATE TABLE 语句）
     *
     * 大模型最擅长理解 DDL 格式，因为它在训练时见过大量的 SQL。
     * 这个方法生成的文本会被塞进提示词中。
     *
     * 输出示例：
     * -- 数据库: test_db
     *
     * CREATE TABLE users (
     *   id INT NOT NULL,  -- 主键
     *   name VARCHAR(50),
     *   age INT,
     *   PRIMARY KEY (id)
     * );
     * -- 用户表
     *
     * CREATE TABLE orders (
     *   id INT NOT NULL,  -- 主键
     *   user_id INT,  -- 外键: user_id -> users.id
     *   amount DECIMAL(10,2),
     *   PRIMARY KEY (id)
     * );
     *
     * @return 格式化后的 DDL 文本字符串
     */
    public String formatAsDDL() {
        StringBuilder sb = new StringBuilder();
        sb.append("-- 数据库: ").append(databaseName);
        if (dbType != null && !dbType.isEmpty()) {
            sb.append(" (").append(dbType).append(")");
        }
        sb.append("\n\n");

        for (TableSchema table : tables) {
            sb.append("CREATE TABLE ").append(table.getTableName()).append(" (\n");

            // 遍历每个列
            List<ColumnInfo> columns = table.getColumns();
            for (int i = 0; i < columns.size(); i++) {
                ColumnInfo col = columns.get(i);
                sb.append("  ").append(col.getColumnName()).append(" ").append(col.getDataType());

                // 如果不允许为空，加上 NOT NULL
                if (!col.isNullable()) {
                    sb.append(" NOT NULL");
                }

                // 添加注释信息（主键/外键/列注释）
                List<String> notes = new ArrayList<>();
                if (table.getPrimaryKeys().contains(col.getColumnName())) {
                    notes.add("主键");
                }
                // 检查是否是外键列
                for (String fk : table.getForeignKeys()) {
                    if (fk.startsWith(col.getColumnName() + " -> ")) {
                        notes.add("外键: " + fk);
                    }
                }
                if (col.getComment() != null && !col.getComment().isEmpty()) {
                    notes.add(col.getComment());
                }
                if (!notes.isEmpty()) {
                    sb.append("  -- ").append(String.join(", ", notes));
                }

                // 除了最后一列，每列后面加逗号
                if (i < columns.size() - 1 || !table.getPrimaryKeys().isEmpty()) {
                    sb.append(",");
                }
                sb.append("\n");
            }

            // 添加主键约束
            if (!table.getPrimaryKeys().isEmpty()) {
                sb.append("  PRIMARY KEY (")
                  .append(String.join(", ", table.getPrimaryKeys()))
                  .append(")\n");
            }

            sb.append(");\n");

            // 表注释
            if (table.getTableComment() != null && !table.getTableComment().isEmpty()) {
                sb.append("-- ").append(table.getTableComment()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
