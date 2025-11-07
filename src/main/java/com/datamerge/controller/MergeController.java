package com.datamerge.controller;

import com.datamerge.model.QueryRequest;
import com.datamerge.model.QueryResponse;
import com.datamerge.service.SparkEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据聚合 REST API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class MergeController {
    
    private final SparkEngineService sparkEngineService;
    
    public MergeController(SparkEngineService sparkEngineService) {
        this.sparkEngineService = sparkEngineService;
    }
    
    /**
     * 执行数据聚合查询
     */
    @PostMapping("/query")
    public QueryResponse query(@RequestBody QueryRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("Received query request: SQL={}", request.getSql());
            
            // 执行查询
            List<Map<String, Object>> result = sparkEngineService.executeQuery(request);
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Query executed successfully in {} ms, returned {} rows", executionTime, result.size());
            
            return QueryResponse.success(result, executionTime);
            
        } catch (Exception e) {
            log.error("Query execution failed", e);
            return QueryResponse.error(e.getMessage());
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Data Merge Engine");
        return response;
    }
}

