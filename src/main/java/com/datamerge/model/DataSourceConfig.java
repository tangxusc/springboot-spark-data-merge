package com.datamerge.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * 数据源配置基类
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = HttpDataSourceConfig.class, name = "http"),
    @JsonSubTypes.Type(value = JsonDataSourceConfig.class, name = "json"),
    @JsonSubTypes.Type(value = ExcelDataSourceConfig.class, name = "excel")
})
public abstract class DataSourceConfig {
    /**
     * 数据源名称（用于创建临时视图）
     */
    private String name;
    
    /**
     * 数据源类型
     */
    private String type;
}

