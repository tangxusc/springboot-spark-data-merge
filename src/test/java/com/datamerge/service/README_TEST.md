# SparkEngineService 独立单元测试

## 概述

本测试是一个**独立于 Spring 容器**的单元测试，使用 **JUnit 5** 和 **Mockito** 来测试 `SparkEngineService.executeQuery()` 方法。

## 测试特点

### ✅ 完全独立
- **不依赖 Spring 容器**：不使用 `@SpringBootTest` 或任何 Spring 测试注解
- **纯 Java 单元测试**：使用 Mockito 模拟所有依赖项
- **快速执行**：无需启动 Spring 容器和 Spark 集群

### ✅ 全面覆盖
测试覆盖了以下场景：

1. **成功执行查询（JSON 数据源）** - `testExecuteQuery_Success_WithJsonDataSource`
   - 验证完整的查询流程
   - 验证结果数据转换
   - 验证临时视图创建

2. **成功执行查询（HTTP 数据源）** - `testExecuteQuery_Success_WithHttpDataSource`
   - 验证参数解析功能
   - 验证 HTTP 头处理
   - 验证 URL 占位符替换

3. **多数据源查询** - `testExecuteQuery_Success_WithMultipleDataSources`
   - 验证多个数据源的加载
   - 验证多个临时视图创建
   - 验证 JOIN 查询

4. **SQL 执行失败** - `testExecuteQuery_Failure_SqlException`
   - 验证异常处理
   - 验证错误消息传递

5. **数据源读取失败** - `testExecuteQuery_Failure_DataSourceReadException`
   - 验证数据源读取异常处理

6. **空结果集** - `testExecuteQuery_Success_EmptyResult`
   - 验证空结果处理

## 技术实现

### Mock 对象
测试使用 Mockito 模拟以下依赖：
- `SparkConfig` - Spark 配置
- `ParameterResolver` - 参数解析器
- `HttpDataSourceReader` - HTTP 数据源读取器
- `JsonDataSourceReader` - JSON 数据源读取器
- `ExcelDataSourceReader` - Excel 数据源读取器
- `SparkSession` - Spark 会话（核心）
- `Dataset<Row>` - Spark 数据集

### 反射注入
通过 Java 反射将 Mock 的 `SparkSession` 注入到服务实例中，避免执行 `@PostConstruct` 初始化方法：

```java
Field sparkSessionField = SparkEngineService.class.getDeclaredField("sparkSession");
sparkSessionField.setAccessible(true);
sparkSessionField.set(sparkEngineService, mockSparkSession);
```

## 运行测试

### Maven
```bash
# 运行单个测试类
mvn test -Dtest=SparkEngineServiceTest

# 运行所有测试
mvn test
```

### IDE
在 IntelliJ IDEA 或 Eclipse 中：
1. 打开 `SparkEngineServiceTest.java`
2. 右键点击类名或测试方法
3. 选择 "Run Test" 或 "Debug Test"

### Gradle（如果使用）
```bash
./gradlew test --tests SparkEngineServiceTest
```

## 验证方式

测试使用 Mockito 的 `verify()` 方法验证交互：

```java
// 验证数据源读取器被调用
verify(mockJsonDataSourceReader, times(1))
    .read(eq(mockSparkSession), any(JsonDataSourceConfig.class));

// 验证 SQL 执行
verify(mockSparkSession, times(1))
    .sql(request.getSql());

// 验证临时视图创建
verify(mockDataset, times(1))
    .createOrReplaceTempView("products");
```

## 注意事项

### 类型安全警告
测试中可能出现一些 Mockito 泛型相关的类型安全警告（例如 `Type safety: The expression of type Map needs unchecked conversion`）。这些是 Java 泛型擦除和 Mockito 的已知问题，**不影响测试功能**。

如果需要消除警告，可以在测试类或方法上添加：
```java
@SuppressWarnings("unchecked")
```

### 依赖版本
确保 `pom.xml` 包含必要的测试依赖：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

该依赖已包含：
- JUnit 5 (Jupiter)
- Mockito
- AssertJ
- Hamcrest

## 扩展测试

如果需要添加更多测试场景，可以参考以下模板：

```java
@Test
public void testExecuteQuery_YourScenario() {
    // 1. 准备测试数据
    QueryRequest request = createQueryRequest();
    
    // 2. 配置 Mock 行为
    when(mockReader.read(...)).thenReturn(mockDataset);
    
    // 3. 执行测试
    List<Map<String, Object>> result = sparkEngineService.executeQuery(request);
    
    // 4. 验证结果
    assertNotNull(result);
    assertEquals(expectedSize, result.size());
    
    // 5. 验证交互
    verify(mockReader, times(1)).read(...);
}
```

## 总结

这个测试展示了如何在**不依赖 Spring 容器**的情况下，使用**纯 Java 和 Mockito** 对复杂的服务类进行单元测试。这种方法具有：
- ✅ **执行速度快**
- ✅ **隔离性好**
- ✅ **易于维护**
- ✅ **便于 CI/CD 集成**

