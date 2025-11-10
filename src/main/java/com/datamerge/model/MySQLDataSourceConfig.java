package com.datamerge.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MySQL 数据库数据源配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MySQLDataSourceConfig extends DataSourceConfig {
    /**
     * 数据库连接 URL，支持 ${paramName} 占位符
     * 格式：jdbc:mysql://host:port/database
     */
    private String url;
    
    /**
     * 数据库用户名，支持 ${paramName} 占位符
     */
    private String username;
    
    /**
     * 数据库密码，支持 ${paramName} 占位符
     */
    private String password;
    
    /**
     * 数据库名称
     */
    private String database;
    
    /**
     * 表名或 SQL 查询语句
     * 如果提供表名，将读取整个表
     * 如果提供 SQL 查询语句，将执行该查询
     */
    private String table;
    
    /**
     * 是否使用自定义 SQL 查询（如果为 true，table 字段将被视为 SQL 查询语句）
     */
    private boolean useCustomQuery = false;
    
    /**
     * 连接池相关配置
     */
    private Integer numPartitions = 10;
    
    /**
     * 分区列（用于并行读取）
     */
    private String partitionColumn;
    
    /**
     * 分区下界
     */
    private String lowerBound;
    
    /**
     * 分区上界
     */
    private String upperBound;
}

