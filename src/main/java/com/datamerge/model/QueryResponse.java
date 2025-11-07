package com.datamerge.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 查询响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse {
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 数据列表
     */
    private List<Map<String, Object>> data;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 执行时间（毫秒）
     */
    private Long executionTime;
    
    public static QueryResponse success(List<Map<String, Object>> data, long executionTime) {
        return new QueryResponse(true, data, null, executionTime);
    }
    
    public static QueryResponse error(String errorMessage) {
        return new QueryResponse(false, null, errorMessage, null);
    }
}

