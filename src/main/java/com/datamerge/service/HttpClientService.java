package com.datamerge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP 客户端服务
 */
@Slf4j
@Service
public class HttpClientService {
    
    private final RestTemplate restTemplate;
    
    public HttpClientService() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * 执行 HTTP 请求
     */
    public String executeRequest(String url, String method, Map<String, String> headers, String body) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            
            HttpEntity<String> entity = new HttpEntity<>(body, httpHeaders);
            
            ResponseEntity<String> response;
            if ("POST".equalsIgnoreCase(method)) {
                response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            } else if ("PUT".equalsIgnoreCase(method)) {
                response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            } else {
                response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            }
            
            log.info("HTTP request successful: {} {}", method, url);
            return response.getBody();
            
        } catch (Exception e) {
            log.error("HTTP request failed: {} {}", method, url, e);
            throw new RuntimeException("Failed to execute HTTP request: " + e.getMessage(), e);
        }
    }
}

