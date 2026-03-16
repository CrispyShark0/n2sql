package com.itheima.n2sql.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据源连接测试的响应 DTO
 *
 * 用户点击"测试连接"后，返回这个结果告诉前端连接是否成功。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceTestResponse {

    /** 连接是否成功 */
    private boolean success;

    /** 提示信息（成功或失败的原因） */
    private String message;

    /** 数据库版本信息（连接成功时返回，如 "MySQL 8.0.33"） */
    private String dbVersion;

    /** 连接耗时（毫秒） */
    private long costTimeMs;

    /**
     * 快速创建一个"连接成功"的响应
     */
    public static DataSourceTestResponse ok(String dbVersion, long costTimeMs) {
        return DataSourceTestResponse.builder()
                .success(true)
                .message("连接成功")
                .dbVersion(dbVersion)
                .costTimeMs(costTimeMs)
                .build();
    }

    /**
     * 快速创建一个"连接失败"的响应
     */
    public static DataSourceTestResponse fail(String reason, long costTimeMs) {
        return DataSourceTestResponse.builder()
                .success(false)
                .message("连接失败: " + reason)
                .costTimeMs(costTimeMs)
                .build();
    }
}
