package com.itheima.n2sql.model.entity;

import com.itheima.n2sql.model.enums.DbType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据源信息实体类
 *
 * 描述一个数据库连接的完整信息，相当于"数据库的名片"。
 * 用户添加数据源时，这些信息会被保存起来，后续连接数据库时使用。
 *
 * Lombok 注解说明：
 *   @Data — 自动生成 getter/setter/toString/equals/hashCode
 *   @Builder — 支持链式构建：DataSourceInfo.builder().name("xx").host("xx").build()
 *   @NoArgsConstructor — 自动生成无参构造方法
 *   @AllArgsConstructor — 自动生成全参构造方法
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceInfo {

    /** 数据源唯一标识（使用 UUID，如 "ds-abc123"） */
    private String id;

    /** 数据源名称（用户自己起的名字，如 "测试数据库"） */
    private String name;

    /** 数据库类型（MYSQL 或 POSTGRESQL） */
    private DbType dbType;

    /** 数据库主机地址（如 "localhost" 或 "192.168.1.100"） */
    private String host;

    /** 数据库端口号（如 MySQL 默认 3306） */
    private int port;

    /** 数据库名称（如 "test_db"） */
    private String dbName;

    /** 数据库用户名 */
    private String username;

    /** 数据库密码 */
    private String password;

    /**
     * 根据当前信息自动生成 JDBC URL
     * 比如 MySQL 会生成：jdbc:mysql://localhost:3306/test_db?useUnicode=true&...
     */
    public String getJdbcUrl() {
        return dbType.buildJdbcUrl(host, port, dbName);
    }
}
