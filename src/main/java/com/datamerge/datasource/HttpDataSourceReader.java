package com.datamerge.datasource;

import com.datamerge.model.HttpDataSourceConfig;
import com.datamerge.service.HttpClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * HTTP API 数据源读取器
 */
@Slf4j
@Component
public class HttpDataSourceReader {
    
    private final HttpClientService httpClientService;
    private final ObjectMapper objectMapper;
    
    public HttpDataSourceReader(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 读取 HTTP API 数据源
     */
    public Dataset<Row> read(SparkSession spark, HttpDataSourceConfig config) {
        try {
            log.info("Reading HTTP data source: {}", config.getUrl());
            
            // 1. 执行 HTTP 请求
            String jsonResponse = httpClientService.executeRequest(
                config.getUrl(),
                config.getMethod(),
                config.getHeaders(),
                config.getBody()
            );
            
            // 2. 解析 JSON 响应
            Object jsonData = objectMapper.readValue(jsonResponse, Object.class);
            
            // 3. 转换为 List（如果是单个对象，包装成数组）
            List<?> dataList;
            if (jsonData instanceof List) {
                dataList = (List<?>) jsonData;
            } else {
                dataList = Collections.singletonList(jsonData);
            }
            
            // 4. 转换为 JSON 字符串并创建 Dataset
            String jsonArray = objectMapper.writeValueAsString(dataList);
            
            // 5. 使用 Spark 的 JSON 读取器
            Dataset<Row> dataset = spark.read().json(
                spark.createDataset(
                    Collections.singletonList(jsonArray),
                    org.apache.spark.sql.Encoders.STRING()
                )
            );
            
            log.info("HTTP data source loaded successfully: {} rows", dataset.count());
            return dataset;
            
        } catch (Exception e) {
            log.error("Failed to read HTTP data source: {}", config.getUrl(), e);
            throw new RuntimeException("Failed to read HTTP data source: " + e.getMessage(), e);
        }
    }
}

