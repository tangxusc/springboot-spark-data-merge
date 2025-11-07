package com.datamerge.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * HTTP API 数据源配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HttpDataSourceConfig extends DataSourceConfig {
    /**
     * API URL，支持 ${paramName} 占位符
     */
    private String url;
    
    /**
     * HTTP 请求头，支持 ${paramName} 占位符
     */
    private Map<String, String> headers;
    
    /**
     * HTTP 方法（GET、POST 等）
     */
    private String method = "GET";
    
    /**
     * 请求体（用于 POST 等方法）
     */
    private String body;
}

