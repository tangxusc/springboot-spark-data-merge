package com.datamerge.service;

import com.datamerge.config.SparkConfig;
import com.datamerge.datasource.ExcelDataSourceReader;
import com.datamerge.datasource.HttpDataSourceReader;
import com.datamerge.datasource.JsonDataSourceReader;
import com.datamerge.model.*;
import com.datamerge.util.ParameterResolver;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SparkEngineService 独立单元测试
 * 不依赖 Spring 容器，使用 Mockito 模拟所有依赖
 */
public class SparkEngineServiceTest {

    private SparkEngineService sparkEngineService;
    
    // Mock 对象
    private SparkConfig mockSparkConfig;
    private ParameterResolver mockParameterResolver;
    private HttpDataSourceReader mockHttpDataSourceReader;
    private JsonDataSourceReader mockJsonDataSourceReader;
    private ExcelDataSourceReader mockExcelDataSourceReader;
    private SparkSession mockSparkSession;
    private Dataset<Row> mockDataset;
    
    @BeforeEach
    public void setUp() throws Exception {
        // 创建 Mock 对象
        mockSparkConfig = mock(SparkConfig.class);
        mockParameterResolver = mock(ParameterResolver.class);
        mockHttpDataSourceReader = mock(HttpDataSourceReader.class);
        mockJsonDataSourceReader = mock(JsonDataSourceReader.class);
        mockExcelDataSourceReader = mock(ExcelDataSourceReader.class);
        mockSparkSession = mock(SparkSession.class);
        mockDataset = mock(Dataset.class);
        
        // 配置 SparkConfig 的默认行为
        when(mockSparkConfig.getAppName()).thenReturn("test-app");
        when(mockSparkConfig.getMaster()).thenReturn("local[*]");
        when(mockSparkConfig.getDriverMemory()).thenReturn("1g");
        when(mockSparkConfig.getExecutorMemory()).thenReturn("1g");
        when(mockSparkConfig.isUiEnabled()).thenReturn(false);
        
        // 创建 SparkEngineService 实例
        sparkEngineService = new SparkEngineService(
            mockSparkConfig,
            mockParameterResolver,
            mockHttpDataSourceReader,
            mockJsonDataSourceReader,
            mockExcelDataSourceReader
        );
        
        // 通过反射注入 Mock 的 SparkSession（避免执行 @PostConstruct）
        Field sparkSessionField = SparkEngineService.class.getDeclaredField("sparkSession");
        sparkSessionField.setAccessible(true);
        sparkSessionField.set(sparkEngineService, mockSparkSession);
    }
    
    /**
     * 测试：成功执行查询（使用 JSON 数据源）
     */
    @Test
    public void testExecuteQuery_Success_WithJsonDataSource() {
        // 准备测试数据
        QueryRequest request = createQueryRequestWithJsonDataSource();
        Dataset<Row> mockResultDataset = createMockResultDataset();
        
        // 配置 Mock 行为
        when(mockJsonDataSourceReader.read(eq(mockSparkSession), any(JsonDataSourceConfig.class)))
            .thenReturn(mockDataset);
        when(mockSparkSession.sql(request.getSql())).thenReturn(mockResultDataset);
        
        // 执行测试
        List<Map<String, Object>> result = sparkEngineService.executeQuery(request);
        
        // 验证结果
        assertNotNull(result, "结果不应该为 null");
        assertEquals(2, result.size(), "应该返回 2 条记录");
        
        // 验证第一条记录
        Map<String, Object> firstRow = result.get(0);
        assertEquals(1, firstRow.get("id"), "第一条记录的 id 应该是 1");
        assertEquals("Product A", firstRow.get("name"), "第一条记录的 name 应该是 'Product A'");
        assertEquals(100.0, firstRow.get("price"), "第一条记录的 price 应该是 100.0");
        
        // 验证第二条记录
        Map<String, Object> secondRow = result.get(1);
        assertEquals(2, secondRow.get("id"), "第二条记录的 id 应该是 2");
        assertEquals("Product B", secondRow.get("name"), "第二条记录的 name 应该是 'Product B'");
        assertEquals(200.0, secondRow.get("price"), "第二条记录的 price 应该是 200.0");
        
        // 验证交互
        verify(mockJsonDataSourceReader, times(1)).read(eq(mockSparkSession), any(JsonDataSourceConfig.class));
        verify(mockSparkSession, times(1)).sql(request.getSql());
        verify(mockDataset, times(1)).createOrReplaceTempView("products");
    }
    
    /**
     * 测试：成功执行查询（使用 HTTP 数据源）
     */
    @Test
    public void testExecuteQuery_Success_WithHttpDataSource() {
        // 准备测试数据
        QueryRequest request = createQueryRequestWithHttpDataSource();
        Dataset<Row> mockResultDataset = createMockResultDataset();
        
        // 配置 Mock 行为 - ParameterResolver
        when(mockParameterResolver.resolve(eq("https://api.example.com/users?id=${userId}"), any(Map.class)))
            .thenReturn("https://api.example.com/users?id=123");
        
        Map<String, String> resolvedHeaders = new HashMap<>();
        resolvedHeaders.put("Authorization", "Bearer token123");
        when(mockParameterResolver.resolveMap(any(Map.class), any(Map.class)))
            .thenReturn(resolvedHeaders);
        
        when(mockHttpDataSourceReader.read(eq(mockSparkSession), any(HttpDataSourceConfig.class)))
            .thenReturn(mockDataset);
        when(mockSparkSession.sql(request.getSql())).thenReturn(mockResultDataset);
        
        // 执行测试
        List<Map<String, Object>> result = sparkEngineService.executeQuery(request);
        
        // 验证结果
        assertNotNull(result, "结果不应该为 null");
        assertEquals(2, result.size(), "应该返回 2 条记录");
        
        // 验证参数解析被调用
        verify(mockParameterResolver, times(1)).resolve(
            eq("https://api.example.com/users?id=${userId}"), 
            any(Map.class)
        );
        verify(mockParameterResolver, times(1)).resolveMap(any(Map.class), any(Map.class));
        
        // 验证 HTTP 数据源读取器被调用
        verify(mockHttpDataSourceReader, times(1)).read(eq(mockSparkSession), any(HttpDataSourceConfig.class));
        
        // 验证 SQL 执行
        verify(mockSparkSession, times(1)).sql(request.getSql());
    }
    
    /**
     * 测试：多个数据源
     */
    @Test
    public void testExecuteQuery_Success_WithMultipleDataSources() {
        // 准备测试数据
        QueryRequest request = createQueryRequestWithMultipleDataSources();
        Dataset<Row> mockResultDataset = createMockResultDataset();
        Dataset<Row> mockDataset2 = mock(Dataset.class);
        
        // 配置 Mock 行为
        when(mockJsonDataSourceReader.read(eq(mockSparkSession), any(JsonDataSourceConfig.class)))
            .thenReturn(mockDataset);
        when(mockHttpDataSourceReader.read(eq(mockSparkSession), any(HttpDataSourceConfig.class)))
            .thenReturn(mockDataset2);
        when(mockParameterResolver.resolve(any(String.class), any(Map.class)))
            .thenReturn("https://api.example.com/users");
        when(mockSparkSession.sql(request.getSql())).thenReturn(mockResultDataset);
        
        // 执行测试
        List<Map<String, Object>> result = sparkEngineService.executeQuery(request);
        
        // 验证结果
        assertNotNull(result, "结果不应该为 null");
        assertEquals(2, result.size(), "应该返回 2 条记录");
        
        // 验证所有数据源都被读取
        verify(mockJsonDataSourceReader, times(1)).read(eq(mockSparkSession), any(JsonDataSourceConfig.class));
        verify(mockHttpDataSourceReader, times(1)).read(eq(mockSparkSession), any(HttpDataSourceConfig.class));
        
        // 验证创建了两个临时视图
        verify(mockDataset, times(1)).createOrReplaceTempView("products");
        verify(mockDataset2, times(1)).createOrReplaceTempView("users");
    }
    
    /**
     * 测试：查询执行失败
     */
    @Test
    public void testExecuteQuery_Failure_SqlException() {
        // 准备测试数据
        QueryRequest request = createQueryRequestWithJsonDataSource();
        
        // 配置 Mock 行为 - 模拟 SQL 执行失败
        when(mockJsonDataSourceReader.read(eq(mockSparkSession), any(JsonDataSourceConfig.class)))
            .thenReturn(mockDataset);
        when(mockSparkSession.sql(request.getSql()))
            .thenThrow(new RuntimeException("SQL syntax error"));
        
        // 执行测试并验证异常
        try {
            sparkEngineService.executeQuery(request);
            fail("应该抛出 RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Query execution failed"), 
                "异常消息应该包含 'Query execution failed'");
            assertTrue(e.getMessage().contains("SQL syntax error"), 
                "异常消息应该包含原始错误信息");
        }
        
        // 验证数据源读取器被调用了
        verify(mockJsonDataSourceReader, times(1)).read(eq(mockSparkSession), any(JsonDataSourceConfig.class));
    }
    
    /**
     * 测试：数据源读取失败
     */
    @Test
    public void testExecuteQuery_Failure_DataSourceReadException() {
        // 准备测试数据
        QueryRequest request = createQueryRequestWithJsonDataSource();
        
        // 配置 Mock 行为 - 模拟数据源读取失败
        when(mockJsonDataSourceReader.read(eq(mockSparkSession), any(JsonDataSourceConfig.class)))
            .thenThrow(new RuntimeException("Failed to read JSON file"));
        
        // 执行测试并验证异常
        try {
            sparkEngineService.executeQuery(request);
            fail("应该抛出 RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Query execution failed"), 
                "异常消息应该包含 'Query execution failed'");
            assertTrue(e.getMessage().contains("Failed to read JSON file"), 
                "异常消息应该包含原始错误信息");
        }
    }
    
    /**
     * 测试：空结果集
     */
    @Test
    public void testExecuteQuery_Success_EmptyResult() {
        // 准备测试数据
        QueryRequest request = createQueryRequestWithJsonDataSource();
        Dataset<Row> mockEmptyDataset = createMockEmptyResultDataset();
        
        // 配置 Mock 行为
        when(mockJsonDataSourceReader.read(eq(mockSparkSession), any(JsonDataSourceConfig.class)))
            .thenReturn(mockDataset);
        when(mockSparkSession.sql(request.getSql())).thenReturn(mockEmptyDataset);
        
        // 执行测试
        List<Map<String, Object>> result = sparkEngineService.executeQuery(request);
        
        // 验证结果
        assertNotNull(result, "结果不应该为 null");
        assertEquals(0, result.size(), "应该返回空列表");
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 创建包含 JSON 数据源的查询请求
     */
    private QueryRequest createQueryRequestWithJsonDataSource() {
        QueryRequest request = new QueryRequest();
        
        JsonDataSourceConfig jsonConfig = new JsonDataSourceConfig();
        jsonConfig.setName("products");
        jsonConfig.setType("json");
        jsonConfig.setPath("/path/to/products.json");
        jsonConfig.setArray(true);
        
        request.setDataSources(Collections.singletonList(jsonConfig));
        request.setSql("SELECT * FROM products");
        request.setParams(new HashMap<>());
        
        return request;
    }
    
    /**
     * 创建包含 HTTP 数据源的查询请求
     */
    private QueryRequest createQueryRequestWithHttpDataSource() {
        QueryRequest request = new QueryRequest();
        
        HttpDataSourceConfig httpConfig = new HttpDataSourceConfig();
        httpConfig.setName("users");
        httpConfig.setType("http");
        httpConfig.setUrl("https://api.example.com/users?id=${userId}");
        httpConfig.setMethod("GET");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer ${token}");
        httpConfig.setHeaders(headers);
        
        request.setDataSources(Collections.singletonList(httpConfig));
        request.setSql("SELECT * FROM users");
        
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 123);
        params.put("token", "token123");
        request.setParams(params);
        
        return request;
    }
    
    /**
     * 创建包含多个数据源的查询请求
     */
    private QueryRequest createQueryRequestWithMultipleDataSources() {
        QueryRequest request = new QueryRequest();
        
        // JSON 数据源
        JsonDataSourceConfig jsonConfig = new JsonDataSourceConfig();
        jsonConfig.setName("products");
        jsonConfig.setType("json");
        jsonConfig.setPath("/path/to/products.json");
        
        // HTTP 数据源
        HttpDataSourceConfig httpConfig = new HttpDataSourceConfig();
        httpConfig.setName("users");
        httpConfig.setType("http");
        httpConfig.setUrl("https://api.example.com/users");
        httpConfig.setMethod("GET");
        
        List<DataSourceConfig> dataSources = Arrays.asList(jsonConfig, httpConfig);
        request.setDataSources(dataSources);
        request.setSql("SELECT * FROM products JOIN users ON products.userId = users.id");
        request.setParams(new HashMap<>());
        
        return request;
    }
    
    /**
     * 创建模拟的结果数据集（包含数据）
     */
    private Dataset<Row> createMockResultDataset() {
        Dataset<Row> mockResult = mock(Dataset.class);
        
        // 模拟列名
        String[] columns = {"id", "name", "price"};
        when(mockResult.columns()).thenReturn(columns);
        
        // 创建模拟的行数据
        Row row1 = RowFactory.create(1, "Product A", 100.0);
        Row row2 = RowFactory.create(2, "Product B", 200.0);
        
        List<Row> rows = Arrays.asList(row1, row2);
        when(mockResult.collectAsList()).thenReturn(rows);
        
        return mockResult;
    }
    
    /**
     * 创建模拟的空结果数据集
     */
    private Dataset<Row> createMockEmptyResultDataset() {
        Dataset<Row> mockResult = mock(Dataset.class);
        
        // 模拟列名
        String[] columns = {"id", "name", "price"};
        when(mockResult.columns()).thenReturn(columns);
        
        // 空行列表
        when(mockResult.collectAsList()).thenReturn(Collections.emptyList());
        
        return mockResult;
    }
}

