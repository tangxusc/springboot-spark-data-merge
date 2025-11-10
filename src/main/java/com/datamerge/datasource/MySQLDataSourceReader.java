package com.datamerge.datasource;

import com.datamerge.model.MySQLDataSourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * MySQL 数据库数据源读取器
 */
@Slf4j
@Component
public class MySQLDataSourceReader {
    
    /**
     * 读取 MySQL 数据源
     */
    public Dataset<Row> read(SparkSession spark, MySQLDataSourceConfig config) {
        try {
            log.info("Reading MySQL data source: {}/{}", config.getUrl(), config.getTable());
            
            // 构建 JDBC URL
            String jdbcUrl = buildJdbcUrl(config);
            
            // 构建读取选项
            Map<String, String> options = new HashMap<>();
            options.put("url", jdbcUrl);
            options.put("user", config.getUsername());
            options.put("password", config.getPassword());
            options.put("driver", "com.mysql.cj.jdbc.Driver");
            
            // 如果使用自定义查询
            if (config.isUseCustomQuery()) {
                options.put("query", config.getTable());
            } else {
                // 使用表名
                String tableName = config.getDatabase() != null && !config.getDatabase().isEmpty()
                    ? config.getDatabase() + "." + config.getTable()
                    : config.getTable();
                options.put("dbtable", tableName);
            }
            
            // 配置分区选项（如果提供了分区信息）
            if (config.getPartitionColumn() != null && 
                config.getLowerBound() != null && 
                config.getUpperBound() != null) {
                options.put("partitionColumn", config.getPartitionColumn());
                options.put("lowerBound", config.getLowerBound());
                options.put("upperBound", config.getUpperBound());
                options.put("numPartitions", String.valueOf(
                    config.getNumPartitions() != null ? config.getNumPartitions() : 10
                ));
            } else if (config.getNumPartitions() != null) {
                options.put("numPartitions", String.valueOf(config.getNumPartitions()));
            }
            
            // 使用 Spark 的 JDBC 读取器读取数据
            Dataset<Row> dataset = spark.read()
                .format("jdbc")
                .options(options)
                .load();
            
            log.info("MySQL data source loaded successfully: {} rows", dataset.count());
            return dataset;
            
        } catch (Exception e) {
            log.error("Failed to read MySQL data source: {}/{}", config.getUrl(), config.getTable(), e);
            throw new RuntimeException("Failed to read MySQL data source: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建 JDBC URL
     */
    private String buildJdbcUrl(MySQLDataSourceConfig config) {
        String url = config.getUrl();
        
        // 如果 URL 不包含 jdbc:mysql:// 前缀，则添加
        if (!url.startsWith("jdbc:mysql://")) {
            if (url.startsWith("mysql://")) {
                url = "jdbc:" + url;
            } else {
                // 假设是 host:port 格式
                url = "jdbc:mysql://" + url;
            }
        }
        
        // 如果 URL 中没有数据库名，且配置中提供了，则添加
        if (config.getDatabase() != null && !config.getDatabase().isEmpty()) {
            if (!url.contains("/") || url.split("/").length < 4) {
                // URL 格式可能不完整，添加数据库名
                if (!url.endsWith("/")) {
                    url += "/";
                }
                url += config.getDatabase();
            }
        }
        
        // 添加常用连接参数
        if (!url.contains("?")) {
            url += "?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8";
        } else if (!url.contains("useSSL")) {
            url += "&useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8";
        }
        
        return url;
    }
}

