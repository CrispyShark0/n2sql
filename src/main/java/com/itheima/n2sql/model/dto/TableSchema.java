package com.itheima.n2sql.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 表结构 DTO
 *
 * 描述一张数据库表的完整结构信息：
 *   - 表名
 *   - 所有列的信息
 *   - 主键列
 *   - 外键关系（本表的哪个列 → 引用了哪张表的哪个列）
 *
 * 举例：orders 表
 *   tableName   = "orders"
 *   columns     = [ColumnInfo("id", "INT", ...), ColumnInfo("user_id", "INT", ...), ...]
 *   primaryKeys = ["id"]
 *   foreignKeys = ["user_id -> users.id"]
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableSchema {

    /** 表名 */
    private String tableName;

    /** 表注释（如果数据库中设置了的话） */
    private String tableComment;

    /** 该表所有列的信息 */
    @Builder.Default
    private List<ColumnInfo> columns = new ArrayList<>();

    /** 主键列名列表（一般只有一个，复合主键时有多个） */
    @Builder.Default
    private List<String> primaryKeys = new ArrayList<>();

    /**
     * 外键关系列表
     * 格式："本表列名 -> 被引用表名.被引用列名"
     * 如："user_id -> users.id"
     */
    @Builder.Default
    private List<String> foreignKeys = new ArrayList<>();
}
