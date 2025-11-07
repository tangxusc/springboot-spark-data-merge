package com.datamerge.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * ParameterResolver 单元测试
 */
class ParameterResolverTest {
    
    private final ParameterResolver parameterResolver = new ParameterResolver();
    
    @Test
    void testResolve_WithSingleParameter() {
        String template = "https://api.example.com/users/${userId}";
        Map<String, Object> params = new HashMap<>();
        params.put("userId", "123");
        
        String result = parameterResolver.resolve(template, params);
        
        assertEquals("https://api.example.com/users/123", result);
    }
    
    @Test
    void testResolve_WithMultipleParameters() {
        String template = "https://api.example.com/users/${userId}/posts/${postId}";
        Map<String, Object> params = new HashMap<>();
        params.put("userId", "123");
        params.put("postId", "456");
        
        String result = parameterResolver.resolve(template, params);
        
        assertEquals("https://api.example.com/users/123/posts/456", result);
    }
    
    @Test
    void testResolve_WithMissingParameter() {
        String template = "https://api.example.com/users/${userId}";
        Map<String, Object> params = new HashMap<>();
        
        String result = parameterResolver.resolve(template, params);
        
        assertEquals("https://api.example.com/users/", result);
    }
    
    @Test
    void testResolve_WithNoPlaceholder() {
        String template = "https://api.example.com/users/123";
        Map<String, Object> params = new HashMap<>();
        params.put("userId", "456");
        
        String result = parameterResolver.resolve(template, params);
        
        assertEquals("https://api.example.com/users/123", result);
    }
    
    @Test
    void testResolveMap() {
        Map<String, String> templateMap = new HashMap<>();
        templateMap.put("Authorization", "Bearer ${token}");
        templateMap.put("X-User-Id", "${userId}");
        
        Map<String, Object> params = new HashMap<>();
        params.put("token", "abc123");
        params.put("userId", "999");
        
        Map<String, String> result = parameterResolver.resolveMap(templateMap, params);
        
        assertNotNull(result);
        assertEquals("Bearer abc123", result.get("Authorization"));
        assertEquals("999", result.get("X-User-Id"));
    }
    
    @Test
    void testResolve_WithNullTemplate() {
        String result = parameterResolver.resolve(null, new HashMap<>());
        assertEquals(null, result);
    }
    
    @Test
    void testResolve_WithNullParams() {
        String template = "https://api.example.com/users/${userId}";
        String result = parameterResolver.resolve(template, null);
        assertEquals(template, result);
    }
}

