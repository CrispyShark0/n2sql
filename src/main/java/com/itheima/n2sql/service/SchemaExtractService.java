package com.itheima.n2sql.service;

import com.itheima.n2sql.config.DataSourceManager;
import com.itheima.n2sql.exception.BizException;
import com.itheima.n2sql.model.dto.ColumnInfo;
import com.itheima.n2sql.model.dto.DatabaseSchema;
import com.itheima.n2sql.model.dto.TableSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库 Schema（结构）自动提取服务
 *
 * 核心功能：连接到指定数据源，自动扫描数据库的所有表结构信息。
 * 提取的信息后续会格式化为 DDL 文本，塞进提示词里让大模型"看懂"数据库。
 *
 * 使用 JDBC 的 DatabaseMetaData API，不需要写 SQL，Java 已经封装好了：
 *   - getTables()       → 获取所有表名
 *   - getColumns()      → 获取某张表的所有列
 *   - getPrimaryKeys()  → 获取主键
 *   - getImportedKeys() → 获取外键关系（本表引用了哪些其他表）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaExtractService {

    /** 数据源管理器，用来获取数据库连接 */
    private final DataSourceManager dataSourceManager;

    /**
     * 提取指定数据源的完整数据库结构
     *
     * @param dataSourceId 数据源ID
     * @return DatabaseSchema 对象，包含所有表的结构信息
     */
    public DatabaseSchema extractSchema(String dataSourceId) {
        // try-with-resources 语法：自动关闭连接，不需要手动写 finally { conn.close() }
        try (Connection conn = dataSourceManager.getConnection(dataSourceId)) {
            DatabaseMetaData metaData = conn.getMetaData();

            // 获取当前连接的数据库名称
            String dbName = conn.getCatalog();
            // PostgreSQL 用 schema 概念，catalog 可能为空，用连接 URL 中的数据库名代替
            if (dbName == null || dbName.isEmpty()) {
                dbName = conn.getSchema();
            }

            // 获取数据库产品名称（如 "MySQL"、"PostgreSQL"），用于方言适配（第五阶段新增）
            String dbProductName = metaData.getDatabaseProductName();

            // 提取所有表的结构
            List<TableSchema> tableSchemas = extractAllTables(metaData, dbName);

            DatabaseSchema schema = DatabaseSchema.builder()
                    .databaseName(dbName)
                    .dbType(dbProductName)
                    .tables(tableSchemas)
                    .build();

            log.info("数据源 [{}] Schema 提取完成，共 {} 张表", dataSourceId, tableSchemas.size());
            return schema;

        } catch (SQLException e) {
            throw new BizException("Schema 提取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 提取数据库中所有表的结构信息
     *
     * @param metaData DatabaseMetaData 对象
     * @param dbName   数据库名称
     * @return 所有表的结构列表
     */
    private List<TableSchema> extractAllTables(DatabaseMetaData metaData, String dbName) throws SQLException {
        List<TableSchema> tables = new ArrayList<>();

        // getTables 参数说明：
        //   参数1: catalog（数据库名），MySQL 用这个
        //   参数2: schemaPattern（模式名），PostgreSQL 用 "public"
        //   参数3: tableNamePattern（表名模式），"%" 表示匹配所有表
        //   参数4: types（表类型），只要普通表 "TABLE"，不要视图
        //
        // 这里同时传 catalog 和 schema，让 MySQL 和 PostgreSQL 都能工作
        ResultSet tableRs = metaData.getTables(dbName, "public", "%", new String[]{"TABLE"});

        while (tableRs.next()) {
            String tableName = tableRs.getString("TABLE_NAME");
            String tableComment = tableRs.getString("REMARKS");  // 表注释

            // 提取这张表的列、主键、外键信息
            List<ColumnInfo> columns = extractColumns(metaData, dbName, tableName);
            List<String> primaryKeys = extractPrimaryKeys(metaData, dbName, tableName);
            List<String> foreignKeys = extractForeignKeys(metaData, dbName, tableName);

            TableSchema tableSchema = TableSchema.builder()
                    .tableName(tableName)
                    .tableComment(tableComment)
                    .columns(columns)
                    .primaryKeys(primaryKeys)
                    .foreignKeys(foreignKeys)
                    .build();

            tables.add(tableSchema);
        }
        tableRs.close();

        return tables;
    }

    /**
     * 提取某张表的所有列信息
     */
    private List<ColumnInfo> extractColumns(DatabaseMetaData metaData, String dbName, String tableName)
            throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();

        ResultSet colRs = metaData.getColumns(dbName, "public", tableName, "%");
        while (colRs.next()) {
            String colName = colRs.getString("COLUMN_NAME");
            String typeName = colRs.getString("TYPE_NAME");
            int columnSize = colRs.getInt("COLUMN_SIZE");
            int decimalDigits = colRs.getInt("DECIMAL_DIGITS");
            boolean nullable = colRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
            String comment = colRs.getString("REMARKS");

            // 拼接完整的类型名，如 VARCHAR(50)、DECIMAL(10,2)
            String fullType = buildFullTypeName(typeName, columnSize, decimalDigits);

            ColumnInfo col = ColumnInfo.builder()
                    .columnName(colName)
                    .dataType(fullType)
                    .nullable(nullable)
                    .comment(comment)
                    .build();

            columns.add(col);
        }
        colRs.close();

        return columns;
    }

    /**
     * 提取某张表的主键列名列表
     */
    private List<String> extractPrimaryKeys(DatabaseMetaData metaData, String dbName, String tableName)
            throws SQLException {
        List<String> pks = new ArrayList<>();

        ResultSet pkRs = metaData.getPrimaryKeys(dbName, "public", tableName);
        while (pkRs.next()) {
            pks.add(pkRs.getString("COLUMN_NAME"));
        }
        pkRs.close();

        return pks;
    }

    /**
     * 提取某张表的外键关系
     *
     * getImportedKeys() 返回的是"本表引用了谁"：
     *   比如 orders 表有 user_id 列，引用了 users 表的 id 列
     *   就会返回：FKCOLUMN_NAME=user_id, PKTABLE_NAME=users, PKCOLUMN_NAME=id
     *
     * 我们格式化为："user_id -> users.id"
     */
    private List<String> extractForeignKeys(DatabaseMetaData metaData, String dbName, String tableName)
            throws SQLException {
        List<String> fks = new ArrayList<>();

        ResultSet fkRs = metaData.getImportedKeys(dbName, "public", tableName);
        while (fkRs.next()) {
            String fkColumn = fkRs.getString("FKCOLUMN_NAME");    // 本表的外键列名
            String pkTable = fkRs.getString("PKTABLE_NAME");      // 被引用的表名
            String pkColumn = fkRs.getString("PKCOLUMN_NAME");    // 被引用的列名

            fks.add(fkColumn + " -> " + pkTable + "." + pkColumn);
        }
        fkRs.close();

        return fks;
    }

    /**
     * 拼接完整的数据类型名称
     *
     * 比如：
     *   typeName="VARCHAR", columnSize=50           → "VARCHAR(50)"
     *   typeName="DECIMAL", columnSize=10, digits=2 → "DECIMAL(10,2)"
     *   typeName="INT",     columnSize=11           → "INT"（整数类型不需要显示长度）
     */
    private String buildFullTypeName(String typeName, int columnSize, int decimalDigits) {
        String upper = typeName.toUpperCase();

        // 这些类型不需要显示长度
        if (upper.contains("INT") || upper.contains("SERIAL") || upper.contains("BOOL")
                || upper.contains("TEXT") || upper.contains("DATE") || upper.contains("TIME")
                || upper.contains("BLOB") || upper.contains("CLOB") || upper.contains("JSON")) {
            return upper;
        }

        // 有小数位的类型，如 DECIMAL(10,2)
        if (decimalDigits > 0) {
            return upper + "(" + columnSize + "," + decimalDigits + ")";
        }

        // 有长度的类型，如 VARCHAR(50)
        if (columnSize > 0 && columnSize < 65535) {
            return upper + "(" + columnSize + ")";
        }

        return upper;
    }
}
