package com.itheima.n2sql.config;

import com.itheima.n2sql.model.entity.DataSourceInfo;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源管理器
 *
 * 负责底层的连接池管理：创建连接池、获取连接、销毁连接池。
 * 使用 HikariCP 作为连接池（Spring Boot 默认推荐，性能最好）。
 *
 * 内部用 ConcurrentHashMap 存储多个数据源：
 *   Key   = 数据源ID（如 "ds-abc123"）
 *   Value = HikariDataSource 连接池对象
 *
 * @Component 注解：告诉 Spring "这个类交给你管理"，其他类可以通过 @Autowired 注入使用
 */
@Slf4j
@Component
public class DataSourceManager {

    /**
     * 存储所有已注册的数据源连接池
     * ConcurrentHashMap 是线程安全的，多个请求同时操作也不会出错
     */
    private final Map<String, HikariDataSource> dataSourcePool = new ConcurrentHashMap<>();

    /**
     * 注册（创建）一个新的数据源连接池
     *
     * @param info 数据源信息（包含地址、端口、账号密码等）
     */
    public void register(DataSourceInfo info) {
        // 如果这个ID已经注册过，先关闭旧的连接池
        if (dataSourcePool.containsKey(info.getId())) {
            log.warn("数据源 [{}] 已存在，将先关闭旧连接池再重新注册", info.getId());
            remove(info.getId());
        }

        // 配置 HikariCP 连接池参数
        HikariConfig config = new HikariConfig();
        config.setPoolName("n2sql-" + info.getId());           // 连接池名称（方便日志识别）
        config.setJdbcUrl(info.getJdbcUrl());                  // JDBC 连接地址
        config.setUsername(info.getUsername());                 // 数据库用户名
        config.setPassword(info.getPassword());                // 数据库密码
        config.setDriverClassName(info.getDbType().getDriverClassName()); // JDBC 驱动类
        config.setMaximumPoolSize(5);                          // 最大连接数（毕设场景5个足够）
        config.setMinimumIdle(1);                              // 最小空闲连接数
        config.setConnectionTimeout(10000);                    // 连接超时：10秒
        config.setIdleTimeout(300000);                         // 空闲超时：5分钟
        config.setMaxLifetime(600000);                         // 连接最大存活时间：10分钟

        // 创建连接池并存入字典
        HikariDataSource dataSource = new HikariDataSource(config);
        dataSourcePool.put(info.getId(), dataSource);
        log.info("数据源 [{}]({}) 注册成功", info.getName(), info.getId());
    }

    /**
     * 获取指定数据源的一个数据库连接
     *
     * @param dataSourceId 数据源ID
     * @return 数据库连接对象
     * @throws SQLException 如果获取连接失败
     */
    public Connection getConnection(String dataSourceId) throws SQLException {
        HikariDataSource ds = dataSourcePool.get(dataSourceId);
        if (ds == null) {
            throw new SQLException("数据源不存在: " + dataSourceId);
        }
        return ds.getConnection();
    }

    /**
     * 获取指定数据源的 DataSource 对象（某些场景需要直接用 DataSource）
     *
     * @param dataSourceId 数据源ID
     * @return DataSource 对象，不存在则返回 null
     */
    public DataSource getDataSource(String dataSourceId) {
        return dataSourcePool.get(dataSourceId);
    }

    /**
     * 移除并关闭一个数据源的连接池
     *
     * @param dataSourceId 数据源ID
     */
    public void remove(String dataSourceId) {
        HikariDataSource ds = dataSourcePool.remove(dataSourceId);
        if (ds != null && !ds.isClosed()) {
            ds.close();  // 关闭连接池，释放所有连接
            log.info("数据源 [{}] 已关闭并移除", dataSourceId);
        }
    }

    /**
     * 检查某个数据源是否已注册
     */
    public boolean contains(String dataSourceId) {
        return dataSourcePool.containsKey(dataSourceId);
    }
}
