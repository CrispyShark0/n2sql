package com.itheima.n2sql.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 列信息 DTO
 *
 * 描述数据库表中的一个列（字段）的详细信息。
 * 比如 users 表的 name 列：
 *   columnName = "name"
 *   dataType   = "VARCHAR(50)"
 *   nullable   = true
 *   comment    = "用户姓名"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnInfo {

    /** 列名，如 "user_id" */
    private String columnName;

    /** 数据类型，如 "VARCHAR(50)"、"INT"、"DECIMAL(10,2)" */
    private String dataType;

    /** 是否允许为空 */
    private boolean nullable;

    /** 列注释（如果数据库中设置了注释的话） */
    private String comment;
}
