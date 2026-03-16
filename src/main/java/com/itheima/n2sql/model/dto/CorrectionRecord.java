package com.itheima.n2sql.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 单轮纠错记录 DTO
 *
 * 第三阶段"自纠错机制"会用到。
 * 每当大模型生成的 SQL 有问题被纠正一次，就产生一条记录。
 *
 * 举例：
 *   第1轮：SQL = "SELECT * FROM user"  → 错误 = "表名不存在，应该是 users"
 *   第2轮：SQL = "SELECT * FROM users" → 成功！
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorrectionRecord {

    /** 该轮生成的 SQL */
    private String sql;

    /** 错误类型（如 "SYNTAX_ERROR"、"EXECUTION_ERROR"、"TABLE_NOT_FOUND"） */
    private String errorType;

    /** 错误详情 */
    private String errorMessage;

    /** 发生时间 */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
