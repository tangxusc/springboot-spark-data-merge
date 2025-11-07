package com.datamerge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Spark 配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spark")
public class SparkConfig {
    
    /**
     * 应用名称
     */
    private String appName = "data-merge-engine";
    
    /**
     * Master URL
     */
    private String master = "local[*]";
    
    /**
     * Driver 内存
     */
    private String driverMemory = "2g";
    
    /**
     * Executor 内存
     */
    private String executorMemory = "2g";
    
    /**
     * 是否启用 UI
     */
    private boolean uiEnabled = false;
}

