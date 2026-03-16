package com.itheima.n2sql.model.enums;

import lombok.Getter;

/**
 * 数据库类型枚举
 *
 * 每种数据库类型都自带：
 *   - 默认端口号
 *   - JDBC URL 模板（用 %s 做占位符，后续自动填充 host、port、dbName）
 *   - JDBC 驱动类名
 *
 * 使用示例：
 *   DbType type = DbType.MYSQL;
 *   String url = type.buildJdbcUrl("localhost", 3306, "mydb");
 *   // 结果："jdbc:mysql://localhost:3306/mydb?useUnicode=true&..."
 */
@Getter
public enum DbType {

    /**
     * MySQL 数据库
     * 默认端口 3306
     */
    MYSQL(
        3306,
        "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
        "com.mysql.cj.jdbc.Driver"
    ),

    /**
     * PostgreSQL 数据库
     * 默认端口 5432
     */
    POSTGRESQL(
        5432,
        "jdbc:postgresql://%s:%d/%s",
        "org.postgresql.Driver"
    );

    /** 默认端口号 */
    private final int defaultPort;

    /** JDBC URL 模板，包含3个 %s/%d 占位符：host、port、dbName */
    private final String urlTemplate;

    /** JDBC 驱动类的全限定名 */
    private final String driverClassName;

    /**
     * 枚举的构造方法（枚举的构造方法默认是 private 的）
     */
    DbType(int defaultPort, String urlTemplate, String driverClassName) {
        this.defaultPort = defaultPort;
        this.urlTemplate = urlTemplate;
        this.driverClassName = driverClassName;
    }

    /**
     * 根据主机、端口、数据库名 拼接出完整的 JDBC URL
     *
     * @param host   数据库主机地址，如 "localhost" 或 "192.168.1.100"
     * @param port   端口号，如 3306
     * @param dbName 数据库名，如 "test_db"
     * @return 完整的 JDBC URL
     */
    public String buildJdbcUrl(String host, int port, String dbName) {
        return String.format(urlTemplate, host, port, dbName);
    }
}
