package com.itheima.n2sql.service;

import com.itheima.n2sql.config.DataSourceManager;
import com.itheima.n2sql.exception.BizException;
import com.itheima.n2sql.model.dto.DataSourceCreateRequest;
import com.itheima.n2sql.model.dto.DataSourceTestResponse;
import com.itheima.n2sql.model.entity.DataSourceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源业务服务层
 *
 * 封装数据源的增删查改 + 测试连接。
 * 数据源信息暂时存在内存中（ConcurrentHashMap），重启后会丢失。
 * 后续如果需要持久化，可以改为存到数据库。
 *
 * @Service 注解：告诉 Spring "这是一个业务服务类"
 * @RequiredArgsConstructor：Lombok注解，自动为所有 final 字段生成构造方法（实现依赖注入）
 *
 * 关于依赖注入（简单理解）：
 *   DataSourceManager 由 Spring 创建和管理，
 *   我们只需要声明 "我需要它"（final字段），Spring 会自动把它"注入"进来。
 *   不需要我们自己 new DataSourceManager()。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceService {

    /** 底层数据源管理器（由 Spring 自动注入） */
    private final DataSourceManager dataSourceManager;

    /**
     * 内存存储：保存所有数据源信息
     * Key = 数据源ID, Value = 数据源信息对象
     */
    private final Map<String, DataSourceInfo> dataSourceStore = new ConcurrentHashMap<>();

    /**
     * 创建一个新的数据源
     *
     * 流程：
     *   1. 生成唯一ID
     *   2. 把请求DTO转成实体对象
     *   3. 保存到内存
     *   4. 在连接池管理器中注册
     *
     * @param request 前端提交的创建请求
     * @return 创建好的数据源信息
     */
    public DataSourceInfo create(DataSourceCreateRequest request) {
        // 1. 生成唯一ID（UUID 是一种全局唯一的随机字符串）
        String id = "ds-" + UUID.randomUUID().toString().substring(0, 8);

        // 2. 如果用户没填端口号，就用该数据库类型的默认端口
        int port = (request.getPort() != null) ? request.getPort() : request.getDbType().getDefaultPort();

        // 3. 构建数据源实体（使用 Builder 模式，链式调用，可读性好）
        DataSourceInfo info = DataSourceInfo.builder()
                .id(id)
                .name(request.getName())
                .dbType(request.getDbType())
                .host(request.getHost())
                .port(port)
                .dbName(request.getDbName())
                .username(request.getUsername())
                .password(request.getPassword())
                .build();

        // 4. 在连接池管理器中注册（创建连接池）— 先注册，成功后再存内存
        try {
            dataSourceManager.register(info);
        } catch (Exception e) {
            // 连接池初始化失败（数据库名错误、密码错误、网络不通等）
            // 不要把错误的数据源存到内存中
            String rootMsg = e.getMessage();
            if (e.getCause() != null) {
                rootMsg = e.getCause().getMessage();
            }
            log.warn("创建数据源失败 [{}]: {}", id, rootMsg);
            throw new BizException("数据源连接失败: " + rootMsg);
        }

        // 5. 连接池创建成功，保存到内存
        dataSourceStore.put(id, info);

        log.info("创建数据源成功: [{}] {}", id, request.getName());
        return info;
    }

    /**
     * 获取所有数据源列表
     */
    public List<DataSourceInfo> listAll() {
        return new ArrayList<>(dataSourceStore.values());
    }

    /**
     * 根据ID获取数据源信息
     *
     * @throws BizException 如果数据源不存在
     */
    public DataSourceInfo getById(String id) {
        DataSourceInfo info = dataSourceStore.get(id);
        if (info == null) {
            throw new BizException(404, "数据源不存在: " + id);
        }
        return info;
    }

    /**
     * 删除一个数据源
     *
     * @param id 数据源ID
     */
    public void delete(String id) {
        DataSourceInfo removed = dataSourceStore.remove(id);
        if (removed == null) {
            throw new BizException(404, "数据源不存在: " + id);
        }
        // 同时关闭连接池
        dataSourceManager.remove(id);
        log.info("删除数据源成功: [{}] {}", id, removed.getName());
    }

    /**
     * 测试数据源连接
     *
     * 流程：
     *   1. 尝试用提供的信息建立数据库连接
     *   2. 获取数据库版本信息
     *   3. 返回测试结果（成功/失败 + 耗时）
     *
     * @param request 数据源信息（和创建请求格式一样）
     * @return 测试结果
     */
    public DataSourceTestResponse testConnection(DataSourceCreateRequest request) {
        long startTime = System.currentTimeMillis();

        // 创建一个临时的数据源信息对象
        int port = (request.getPort() != null) ? request.getPort() : request.getDbType().getDefaultPort();
        DataSourceInfo tempInfo = DataSourceInfo.builder()
                .id("temp-test")
                .name("临时测试")
                .dbType(request.getDbType())
                .host(request.getHost())
                .port(port)
                .dbName(request.getDbName())
                .username(request.getUsername())
                .password(request.getPassword())
                .build();

        try {
            // 临时注册，测试完就删除
            dataSourceManager.register(tempInfo);
            // 尝试获取连接
            Connection conn = dataSourceManager.getConnection("temp-test");
            // 获取数据库版本信息
            DatabaseMetaData metaData = conn.getMetaData();
            String dbVersion = metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion();
            // 关闭连接（归还到连接池）
            conn.close();

            long costTime = System.currentTimeMillis() - startTime;
            log.info("数据源连接测试成功: {} ({}ms)", dbVersion, costTime);
            return DataSourceTestResponse.ok(dbVersion, costTime);

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.warn("数据源连接测试失败: {}", e.getMessage());
            return DataSourceTestResponse.fail(e.getMessage(), costTime);

        } finally {
            // 无论成功失败，都要清理临时数据源
            dataSourceManager.remove("temp-test");
        }
    }
}
