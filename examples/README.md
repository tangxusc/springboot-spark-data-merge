# 示例文件说明

本目录包含数据聚合引擎的示例文件和测试数据。

## 文件说明

### 数据文件

- `sample-products.json` - 示例产品数据（JSON 格式）

### 查询示例

- `query-example-http.json` - HTTP API 数据源查询示例
- `query-example-json.json` - JSON 文件数据源查询示例
- `query-example-with-params.json` - 带参数传递的复杂查询示例

## 使用方法

### 1. 启动应用

```bash
cd ..
mvn spring-boot:run
```

### 2. 执行查询

#### 查询 HTTP API 数据源

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d @examples/query-example-http.json
```

#### 查询 JSON 文件数据源

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d @examples/query-example-json.json
```

#### 查询带参数的示例

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d @examples/query-example-with-params.json
```

## 预期输出

所有查询应返回类似以下格式的响应：

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "产品名称",
      "price": 999.00
    }
  ],
  "errorMessage": null,
  "executionTime": 1523
}
```

