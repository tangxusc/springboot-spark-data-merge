package com.datamerge.util;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参数解析器 - 替换占位符 ${paramName}
 */
@Component
public class ParameterResolver {
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    /**
     * 替换字符串中的占位符
     */
    public String resolve(String text, Map<String, Object> params) {
        if (text == null || params == null || params.isEmpty()) {
            return text;
        }
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object paramValue = params.get(paramName);
            String replacement = paramValue != null ? paramValue.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * 替换 Map 中所有值的占位符
     */
    public Map<String, String> resolveMap(Map<String, String> map, Map<String, Object> params) {
        if (map == null || map.isEmpty()) {
            return map;
        }
        
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            result.put(entry.getKey(), resolve(entry.getValue(), params));
        }
        
        return result;
    }
}

