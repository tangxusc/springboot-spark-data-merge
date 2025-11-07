package com.datamerge.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 查询请求
 */
@Data
public class QueryRequest {
    /**
     * 数据源配置列表
     */
    private List<DataSourceConfig> dataSources;
    
    /**
     * SQL 查询语句
     */
    private String sql;
    
    /**
     * 参数映射（用于替换占位符）
     */
    private Map<String, Object> params;
}

