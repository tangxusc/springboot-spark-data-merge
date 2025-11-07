# API 文档

## 基础信息

- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **字符编码**: UTF-8

## 接口列表

### 1. 健康检查

检查服务是否正常运行。

#### 请求

```
GET /api/health
```

#### 响应

```json
{
  "status": "UP",
  "service": "Data Merge Engine"
}
```

#### 状态码

- `200 OK` - 服务正常

---

### 2. 执行数据聚合查询

执行数据聚合查询，支持多数据源 JOIN。

#### 请求

```
POST /api/query
Content-Type: application/json
```

#### 请求体结构

```json
{
  "dataSources": [DataSourceConfig],
  "sql": "string",
  "params": {
    "key": "value"
  }
}
```

##### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| dataSources | Array | 是 | 数据源配置列表 |
| sql | String | 是 | Spark SQL 查询语句 |
| params | Object | 否 | 参数映射，用于替换占位符 |

#### 数据源配置

##### HTTP API 数据源

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

**字段说明**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 临时视图名称（用于 SQL 查询） |
| type | String | 是 | 固定值 "http" |
| url | String | 是 | API URL，支持 `${paramName}` 占位符 |
| method | String | 否 | HTTP 方法，默认 GET |
| headers | Object | 否 | 请求头，支持 `${paramName}` 占位符 |
| body | String | 否 | 请求体（用于 POST/PUT） |

##### JSON 文件数据源

```json
{
  "name": "products",
  "type": "json",
  "path": "/path/to/products.json",
  "isArray": true
}
```

**字段说明**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 临时视图名称 |
| type | String | 是 | 固定值 "json" |
| path | String | 是 | JSON 文件路径 |
| isArray | Boolean | 否 | 是否为数组，默认 true |

##### Excel 文件数据源

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

**字段说明**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 临时视图名称 |
| type | String | 是 | 固定值 "excel" |
| path | String | 是 | Excel 文件路径 |
| sheet | String | 否 | Sheet 名称，默认 "Sheet1" |
| hasHeader | Boolean | 否 | 是否包含表头，默认 true |
| startRow | Integer | 否 | 起始行（0-based），默认 0 |

#### 响应

##### 成功响应

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "John Doe",
      "email": "john@example.com"
    }
  ],
  "errorMessage": null,
  "executionTime": 1523
}
```

##### 错误响应

```json
{
  "success": false,
  "data": null,
  "errorMessage": "Query execution failed: ...",
  "executionTime": null
}
```

##### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| success | Boolean | 查询是否成功 |
| data | Array | 查询结果数据（Map 数组） |
| errorMessage | String | 错误信息（仅失败时） |
| executionTime | Long | 执行时间（毫秒） |

#### 状态码

- `200 OK` - 请求成功（包括查询失败的情况，通过 success 字段判断）
- `400 Bad Request` - 请求参数错误
- `500 Internal Server Error` - 服务器内部错误

---

## 使用示例

### 示例 1: 查询单个 HTTP API

#### 请求

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "dataSources": [
      {
        "name": "users",
        "type": "http",
        "url": "https://jsonplaceholder.typicode.com/users"
      }
    ],
    "sql": "SELECT id, name, email FROM users WHERE id <= 3"
  }'
```

#### 响应

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Leanne Graham",
      "email": "Sincere@april.biz"
    },
    {
      "id": 2,
      "name": "Ervin Howell",
      "email": "Shanna@melissa.tv"
    },
    {
      "id": 3,
      "name": "Clementine Bauch",
      "email": "Nathan@yesenia.net"
    }
  ],
  "errorMessage": null,
  "executionTime": 1234
}
```

### 示例 2: 多数据源 JOIN 查询

#### 请求

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "dataSources": [
      {
        "name": "users",
        "type": "http",
        "url": "https://jsonplaceholder.typicode.com/users/${userId}",
        "headers": {
          "Authorization": "Bearer ${token}"
        }
      },
      {
        "name": "posts",
        "type": "http",
        "url": "https://jsonplaceholder.typicode.com/posts?userId=${userId}"
      }
    ],
    "sql": "SELECT u.name, u.email, COUNT(p.id) as post_count FROM users u LEFT JOIN posts p ON u.id = p.userId GROUP BY u.name, u.email",
    "params": {
      "userId": "1",
      "token": "your-api-token"
    }
  }'
```

### 示例 3: JSON 文件数据源

#### 请求

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "dataSources": [
      {
        "name": "products",
        "type": "json",
        "path": "examples/sample-products.json"
      }
    ],
    "sql": "SELECT name, price FROM products WHERE price > 100 ORDER BY price DESC"
  }'
```

### 示例 4: Excel 文件数据源

#### 请求

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "dataSources": [
      {
        "name": "employees",
        "type": "excel",
        "path": "/data/employees.xlsx",
        "sheet": "员工信息",
        "hasHeader": true
      }
    ],
    "sql": "SELECT * FROM employees WHERE department = \"研发部\""
  }'
```

### 示例 5: 复杂聚合查询

#### 请求

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "dataSources": [
      {
        "name": "orders",
        "type": "http",
        "url": "https://api.example.com/orders"
      },
      {
        "name": "products",
        "type": "json",
        "path": "/data/products.json"
      },
      {
        "name": "categories",
        "type": "excel",
        "path": "/data/categories.xlsx"
      }
    ],
    "sql": "SELECT c.name as category, COUNT(o.id) as order_count, SUM(o.amount) as total_amount FROM orders o JOIN products p ON o.product_id = p.id JOIN categories c ON p.category_id = c.id GROUP BY c.name ORDER BY total_amount DESC"
  }'
```

## Spark SQL 支持

本引擎使用 Spark SQL 3.3.2，支持标准 SQL 语法和 Spark SQL 扩展功能。

### 支持的操作

- **基本查询**: SELECT, FROM, WHERE
- **聚合函数**: COUNT, SUM, AVG, MIN, MAX
- **分组**: GROUP BY, HAVING
- **排序**: ORDER BY
- **连接**: INNER JOIN, LEFT JOIN, RIGHT JOIN, FULL OUTER JOIN
- **子查询**: 标准 SQL 子查询
- **窗口函数**: ROW_NUMBER, RANK, DENSE_RANK 等
- **条件表达式**: CASE WHEN
- **字符串函数**: CONCAT, SUBSTRING, UPPER, LOWER 等
- **日期函数**: CURRENT_DATE, DATE_ADD, DATE_SUB 等

### SQL 示例

#### 窗口函数

```sql
SELECT 
  name, 
  price, 
  ROW_NUMBER() OVER (ORDER BY price DESC) as rank
FROM products
```

#### CASE WHEN

```sql
SELECT 
  name,
  CASE 
    WHEN price > 1000 THEN '高价'
    WHEN price > 500 THEN '中价'
    ELSE '低价'
  END as price_level
FROM products
```

## 错误码说明

### 业务错误

| 错误信息 | 说明 | 解决方案 |
|----------|------|----------|
| Invalid request: ... | 请求参数不合法 | 检查请求体格式和必填字段 |
| Query execution failed: ... | SQL 查询执行失败 | 检查 SQL 语法和临时视图名称 |
| Failed to read ... data source | 数据源读取失败 | 检查数据源配置和文件路径 |
| Failed to execute HTTP request | HTTP 请求失败 | 检查 URL 和网络连接 |

### HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功（需检查 success 字段） |
| 400 | 请求参数错误 |
| 500 | 服务器内部错误 |

## 性能建议

1. **限制结果数量**: 使用 `LIMIT` 子句限制返回行数
2. **避免全表扫描**: 在 WHERE 子句中使用条件过滤
3. **合理使用 JOIN**: 大表 JOIN 可能导致性能问题
4. **调整 Spark 配置**: 根据数据量调整内存配置

## 安全建议

1. **Token 管理**: 敏感的 Token 应通过环境变量传递
2. **文件路径限制**: 限制可访问的文件路径范围
3. **SQL 注入防护**: 使用参数化查询
4. **访问控制**: 实现 API 认证和授权机制

## 限制说明

1. 单次查询返回数据量建议不超过 10000 行
2. HTTP API 响应大小建议不超过 10MB
3. Excel 文件建议不超过 100MB
4. 并发查询数量受 Spark 资源限制

