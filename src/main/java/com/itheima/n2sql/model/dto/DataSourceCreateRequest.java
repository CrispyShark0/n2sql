package com.itheima.n2sql.model.dto;

import com.itheima.n2sql.model.enums.DbType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建数据源的请求体 DTO
 *
 * 前端调用"新增数据源"接口时，需要传这些字段。
 * 加了 @NotBlank/@NotNull 的字段是必填的，不填会被参数校验拦截。
 *
 * DTO 和 Entity 的区别：
 *   - Entity（实体）：对应数据库中的一条记录，包含 id 等系统字段
 *   - DTO（数据传输对象）：只包含前端需要传/收的字段，更安全（不暴露内部结构）
 */
@Data
public class DataSourceCreateRequest {

    /** 数据源名称（必填） */
    @NotBlank(message = "数据源名称不能为空")
    private String name;

    /** 数据库类型（必填）：MYSQL 或 POSTGRESQL */
    @NotNull(message = "数据库类型不能为空")
    private DbType dbType;

    /** 主机地址（必填） */
    @NotBlank(message = "主机地址不能为空")
    private String host;

    /** 端口号（不填则使用数据库类型的默认端口） */
    private Integer port;

    /** 数据库名称（必填） */
    @NotBlank(message = "数据库名称不能为空")
    private String dbName;

    /** 用户名（必填） */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码（必填） */
    @NotBlank(message = "密码不能为空")
    private String password;
}
