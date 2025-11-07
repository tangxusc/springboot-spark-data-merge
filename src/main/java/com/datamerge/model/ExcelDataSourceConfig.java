package com.datamerge.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Excel 文件数据源配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExcelDataSourceConfig extends DataSourceConfig {
    /**
     * Excel 文件路径
     */
    private String path;
    
    /**
     * Sheet 名称
     */
    private String sheet = "Sheet1";
    
    /**
     * 是否包含表头
     */
    private boolean hasHeader = true;
    
    /**
     * 起始行（0-based）
     */
    private int startRow = 0;
}

