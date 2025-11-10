# 数据聚合引擎 (Data Merge Engine)

基于 Spring Boot 2.7 和嵌入式 Spark SQL 构建的数据聚合引擎，支持聚合 HTTP API、JSON 文件、Excel 文件、MySQL 数据库等多种数据源，并通过 REST 接口返回聚合结果。

## 功能特性

- **多数据源支持**：HTTP API、JSON 文件、Excel 文件、MySQL 数据库
- **Spark SQL 聚合**：使用嵌入式 Spark SQL 引擎执行复杂的数据聚合
- **参数传递**：支持从 REST API 传入参数到数据源（URL、Headers、数据库连接信息等）
- **Token 认证**：支持 HTTP API 数据源的 Token 认证
- **REST API**：提供简洁的 REST 接口进行数据查询

## 技术栈

- Spring Boot 2.7
- Apache Spark 3.3.2 (嵌入式模式)
- Spring Cloud OpenFeign
- Apache POI (Excel 解析)
- Jackson (JSON 处理)
- MySQL Connector/J (MySQL 数据库连接)
- Maven

## 快速开始

### 前置要求

- JDK 1.8+
- Maven 3.6+

### 构建项目

```bash
mvn clean package
```

### 运行应用

```bash
java -jar target/data-merge-engine-1.0.0.jar
```

应用将在 `http://localhost:8080` 启动。

## API 使用示例

### 1. 健康检查

```bash
curl http://localhost:8080/api/health
```

### 2. 执行数据聚合查询

#### 请求示例：聚合 HTTP API 和 Excel 数据

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "dataSources": [
      {
        "name": "users",
        "type": "http",
        "url": "https://jsonplaceholder.typicode.com/users/${userId}",
        "method": "GET",
        "headers": {
          "Authorization": "Bearer ${token}"
        }
      },
      {
        "name": "config",
        "type": "excel",
        "path": "/path/to/config.xlsx",
        "sheet": "Sheet1",
        "hasHeader": true
      }
    ],
    "sql": "SELECT u.*, c.value FROM users u LEFT JOIN config c ON u.id = c.user_id",
    "params": {
      "userId": "1",
      "token": "your-api-token"
    }
  }'
```

#### 请求示例：聚合 JSON 文件

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "dataSources": [
      {
        "name": "products",
        "type": "json",
        "path": "/path/to/products.json",
        "isArray": true
      }
    ],
    "sql": "SELECT * FROM products WHERE price > 100",
    "params": {}
  }'
```

#### 请求示例：聚合 MySQL 数据库数据

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "dataSources": [
      {
        "name": "users",
        "type": "mysql",
        "url": "jdbc:mysql://localhost:3306/mydb",
        "username": "root",
        "password": "password",
        "database": "mydb",
        "table": "users"
      }
    ],
    "sql": "SELECT * FROM users WHERE age > 18",
    "params": {}
  }'
```

#### 请求示例：使用自定义 SQL 查询 MySQL 数据

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "dataSources": [
      {
        "name": "user_stats",
        "type": "mysql",
        "url": "jdbc:mysql://localhost:3306/mydb",
        "username": "root",
        "password": "password",
        "database": "mydb",
        "table": "SELECT u.id, u.name, COUNT(o.id) as order_count FROM users u LEFT JOIN orders o ON u.id = o.user_id GROUP BY u.id, u.name",
        "useCustomQuery": true
      }
    ],
    "sql": "SELECT * FROM user_stats WHERE order_count > 5",
    "params": {}
  }'
```

#### 请求示例：聚合多个数据源（MySQL + HTTP API）

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "dataSources": [
      {
        "name": "db_users",
        "type": "mysql",
        "url": "jdbc:mysql://localhost:3306/mydb",
        "username": "root",
        "password": "password",
        "database": "mydb",
        "table": "users"
      },
      {
        "name": "api_orders",
        "type": "http",
        "url": "https://api.example.com/orders",
        "method": "GET",
        "headers": {
          "Authorization": "Bearer ${token}"
        }
      }
    ],
    "sql": "SELECT u.id, u.name, COUNT(o.id) as order_count FROM db_users u LEFT JOIN api_orders o ON u.id = o.user_id GROUP BY u.id, u.name",
    "params": {
      "token": "your-api-token"
    }
  }'
```

#### 响应示例

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "John Doe",
      "email": "john@example.com",
      "value": "config_value"
    }
  ],
  "errorMessage": null,
  "executionTime": 1523
}
```

## 配置说明

### application.yml

```yaml
server:
  port: 8080

spark:
  app-name: data-merge-engine
  master: local[*]           # 嵌入式模式
  driver-memory: 2g          # Driver 内存
  executor-memory: 2g        # Executor 内存
  ui-enabled: false          # 禁用 Spark UI
  log-level: WARN            # 日志级别

feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
```

## 数据源配置

### HTTP API 数据源

```json
{
  "name": "users",
  "type": "http",
  "url": "https://api.example.com/users/${userId}",
  "method": "GET",
  "headers": {
    "Authorization": "Bearer ${token}",
    "Content-Type": "application/json"
  },
  "body": "{\"key\": \"value\"}"
}
```

- `url`: 支持 `${paramName}` 占位符
- `headers`: 支持 `${paramName}` 占位符
- `method`: GET、POST、PUT 等

### JSON 文件数据源

```json
{
  "name": "products",
  "type": "json",
  "path": "/path/to/products.json",
  "isArray": true
}
```

### Excel 文件数据源

```json
{
  "name": "config",
  "type": "excel",
  "path": "/path/to/config.xlsx",
  "sheet": "Sheet1",
  "hasHeader": true,
  "startRow": 0
}
```

### MySQL 数据库数据源

#### 基本配置（使用表名）

```json
{
  "name": "users",
  "type": "mysql",
  "url": "jdbc:mysql://localhost:3306/mydb",
  "username": "root",
  "password": "password",
  "database": "mydb",
  "table": "users"
}
```

#### 使用自定义 SQL 查询

```json
{
  "name": "user_stats",
  "type": "mysql",
  "url": "jdbc:mysql://localhost:3306/mydb",
  "username": "root",
  "password": "password",
  "database": "mydb",
  "table": "SELECT u.id, u.name, COUNT(o.id) as order_count FROM users u LEFT JOIN orders o ON u.id = o.user_id GROUP BY u.id, u.name",
  "useCustomQuery": true
}
```

#### 使用参数占位符

```json
{
  "name": "users",
  "type": "mysql",
  "url": "jdbc:mysql://${host}:3306/${database}",
  "username": "${dbUser}",
  "password": "${dbPassword}",
  "database": "${database}",
  "table": "users"
}
```

#### 配置分区读取（提高大数据量读取性能）

```json
{
  "name": "large_table",
  "type": "mysql",
  "url": "jdbc:mysql://localhost:3306/mydb",
  "username": "root",
  "password": "password",
  "database": "mydb",
  "table": "large_table",
  "numPartitions": 10,
  "partitionColumn": "id",
  "lowerBound": "1",
  "upperBound": "1000000"
}
```

**配置说明：**

- `url`: JDBC 连接 URL，支持 `${paramName}` 占位符。如果只提供 `host:port`，系统会自动添加 `jdbc:mysql://` 前缀
- `username`: 数据库用户名，支持 `${paramName}` 占位符
- `password`: 数据库密码，支持 `${paramName}` 占位符
- `database`: 数据库名称（可选，如果 URL 中已包含则不需要）
- `table`: 表名或自定义 SQL 查询语句
- `useCustomQuery`: 是否使用自定义 SQL 查询（默认 `false`）。如果为 `true`，`table` 字段将被视为 SQL 查询语句
- `numPartitions`: 分区数量（可选，默认 10），用于并行读取
- `partitionColumn`: 分区列名（可选），用于并行读取
- `lowerBound`: 分区下界（可选），配合 `partitionColumn` 使用
- `upperBound`: 分区上界（可选），配合 `partitionColumn` 使用

## 项目结构

```
src/main/java/com/datamerge/
├── config/                    # 配置类
│   ├── SparkConfig.java      # Spark 配置
│   └── FeignConfig.java      # Feign 配置
├── controller/                # REST API 控制器
│   └── MergeController.java
├── datasource/                # 数据源读取器
│   ├── HttpDataSourceReader.java
│   ├── JsonDataSourceReader.java
│   ├── ExcelDataSourceReader.java
│   └── MySQLDataSourceReader.java
├── model/                     # 数据模型
│   ├── DataSourceConfig.java
│   ├── HttpDataSourceConfig.java
│   ├── JsonDataSourceConfig.java
│   ├── ExcelDataSourceConfig.java
│   ├── MySQLDataSourceConfig.java
│   ├── QueryRequest.java
│   └── QueryResponse.java
├── service/                   # 核心服务
│   ├── SparkEngineService.java
│   └── HttpClientService.java
├── util/                      # 工具类
│   └── ParameterResolver.java
├── exception/                 # 异常处理
│   └── GlobalExceptionHandler.java
└── DataMergeEngineApplication.java
```

## 故障排除

如果遇到问题，请参考 [TROUBLESHOOTING.md](TROUBLESHOOTING.md) 文档，其中包含常见问题的解决方案。

常见问题：
- 日志框架冲突错误 - 已在代码中修复
- Windows 系统 Hadoop 警告 - 可以安全忽略
- 内存不足 - 调整 JVM 和 Spark 内存配置
- HTTP 请求超时 - 增加 Feign 超时时间

## 许可证

本项目采用 MIT 许可证。

