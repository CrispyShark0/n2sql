package com.itheima.n2sql.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * NL2SQL 响应体
 *
 * 返回给前端的完整结果，包含：
 *   - 生成的 SQL 语句
 *   - SQL 执行结果（如果执行了的话）
 *   - 纠错信息（第三阶段会用到）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Nl2SqlResponse {

    /** 用户原始问题 */
    private String question;

    /** 大模型生成的 SQL 语句 */
    private String generatedSql;

    /** SQL 执行结果（第 2.4 步实现后会填充） */
    private QueryResult queryResult;

    /** 是否执行成功 */
    private boolean success;

    /** 错误信息（如果失败的话） */
    private String errorMessage;

    /** 自纠错重试次数（第三阶段会用到） */
    @Builder.Default
    private int retryCount = 0;

    /** 纠错历史记录（第三阶段会用到，记录每轮的 SQL 和错误） */
    @Builder.Default
    private List<CorrectionRecord> correctionHistory = new ArrayList<>();
}
