package com.datamerge.datasource;

import com.datamerge.model.JsonDataSourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Component;

/**
 * JSON 文件数据源读取器
 */
@Slf4j
@Component
public class JsonDataSourceReader {
    
    /**
     * 读取 JSON 文件数据源
     */
    public Dataset<Row> read(SparkSession spark, JsonDataSourceConfig config) {
        try {
            log.info("Reading JSON data source: {}", config.getPath());
            
            Dataset<Row> dataset;
            if (config.isArray()) {
                // 读取 JSON 数组
                dataset = spark.read().json(config.getPath());
            } else {
                // 读取单个 JSON 对象（每行一个 JSON）
                dataset = spark.read().json(config.getPath());
            }
            
            log.info("JSON data source loaded successfully: {} rows", dataset.count());
            return dataset;
            
        } catch (Exception e) {
            log.error("Failed to read JSON data source: {}", config.getPath(), e);
            throw new RuntimeException("Failed to read JSON data source: " + e.getMessage(), e);
        }
    }
}

