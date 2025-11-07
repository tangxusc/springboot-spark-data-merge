package com.datamerge.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * JSON 文件数据源配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JsonDataSourceConfig extends DataSourceConfig {
    /**
     * JSON 文件路径
     */
    private String path;
    
    /**
     * 是否为 JSON 数组
     */
    private boolean isArray = true;
}

