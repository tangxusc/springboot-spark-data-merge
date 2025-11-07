# 故障排除指南

本文档记录了数据聚合引擎开发和使用过程中遇到的常见问题及解决方案。

---

## 1. 日志框架冲突错误（已修复）

### 问题描述

启动应用时出现以下错误：

```
java.lang.ClassCastException: org.apache.logging.slf4j.SLF4JLoggerContext cannot be cast to org.apache.logging.log4j.core.LoggerContext
    at org.apache.spark.util.Utils$.setLogLevel(Utils.scala:2463)
    at org.apache.spark.SparkContext.setLogLevel(SparkContext.scala:387)
```

或者：

```
java.lang.NoClassDefFoundError: org/apache/logging/log4j/core/Filter
```

### 原因分析

- Spring Boot 默认使用 **Logback** (SLF4J 实现)
- Apache Spark 内部依赖 **Log4j2**
- 日志框架冲突导致类加载失败

### 最终解决方案（已实施）

#### 方案：只排除冲突的绑定，保留核心依赖

在 `pom.xml` 中的配置（当前实施的方案）：

```xml
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-core_${scala.binary.version}</artifactId>
    <version>${spark.version}</version>
    <exclusions>
        <!-- 只排除 slf4j-log4j12 -->
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-log4j12</artifactId>
        </exclusion>
        <!-- 排除 log4j-slf4j-impl 避免冲突 -->
        <exclusion>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-slf4j-impl</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 添加 Log4j 2 to SLF4J 适配器 -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-to-slf4j</artifactId>
</dependency>
```

**关键点**：
- ❌ 不要排除 `log4j-api` 和 `log4j-core`（Spark 运行时需要）
- ✅ 只排除会导致绑定冲突的 `log4j-slf4j-impl`
- ✅ 添加 `log4j-to-slf4j` 让所有日志统一到 SLF4J/Logback

#### 移除 setLogLevel 调用

在 `SparkEngineService.java` 中不调用会导致冲突的方法：

```java
// ❌ 不要调用这个
// sparkSession.sparkContext().setLogLevel("WARN");

// ✅ 日志级别通过 application.yml 配置
```

### 验证修复

重新编译并启动：

```bash
mvn clean package
java -jar target/data-merge-engine-1.0.0.jar
```

应用应该可以正常启动，不再出现 ClassCastException。

---

## 2. Windows 系统 Hadoop 警告

### 问题描述

在 Windows 上启动时出现警告：

```
WARN org.apache.hadoop.util.Shell: Did not find winutils.exe
java.io.FileNotFoundException: HADOOP_HOME and hadoop.home.dir are unset
```

### 原因分析

Spark 依赖 Hadoop，在 Windows 上需要 `winutils.exe`。

### 解决方案

#### 方案 1: 忽略警告（推荐）

这只是一个警告，不影响功能。嵌入式模式下可以安全忽略。

#### 方案 2: 下载 winutils.exe

1. 下载 [winutils.exe](https://github.com/steveloughran/winutils)
2. 设置环境变量：`HADOOP_HOME=C:\hadoop`
3. 将 `winutils.exe` 放到 `C:\hadoop\bin\` 目录

---

## 3. 内存不足错误

### 问题描述

```
java.lang.OutOfMemoryError: Java heap space
```

### 解决方案

#### 1. 增加 JVM 内存

```bash
java -Xmx4g -jar target/data-merge-engine-1.0.0.jar
```

#### 2. 调整 Spark 配置

在 `application.yml` 中增加内存：

```yaml
spark:
  driver-memory: 4g
  executor-memory: 4g
```

#### 3. 限制查询结果

在 SQL 中使用 `LIMIT` 限制返回行数：

```sql
SELECT * FROM users LIMIT 1000
```

---

## 4. HTTP 请求超时

### 问题描述

```
java.net.SocketTimeoutException: Read timed out
```

### 解决方案

增加 Feign 超时时间：

```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 10000
        readTimeout: 30000
```

---

## 5. Excel 文件读取失败

### 问题描述

```
java.io.FileNotFoundException: /path/to/file.xlsx
```

### 解决方案

1. **检查文件路径**：使用绝对路径或相对于项目根目录的路径
2. **检查文件权限**：确保应用有读取权限
3. **验证文件格式**：确保是有效的 .xls 或 .xlsx 文件

---

## 6. Spark SQL 语法错误

### 问题描述

```
org.apache.spark.sql.AnalysisException: cannot resolve 'xxx' given input columns
```

### 常见原因

1. **临时视图名称错误**：SQL 中的表名必须与数据源的 `name` 字段匹配
2. **列名错误**：检查列名拼写和大小写
3. **视图未创建**：确保数据源加载成功

### 解决方案

1. 检查临时视图名称：

```json
{
  "name": "users",  // ← 这个名称
  "type": "http",
  "url": "..."
}
```

```sql
SELECT * FROM users  -- ← 必须匹配上面的名称
```

2. 查看可用列：在开发时可以使用 `SELECT *` 查看所有列

---

## 7. JSON 解析错误

### 问题描述

```
com.fasterxml.jackson.core.JsonParseException: Unexpected character
```

### 解决方案

1. **验证 JSON 格式**：使用 JSON 验证工具检查语法
2. **检查编码**：确保文件是 UTF-8 编码
3. **处理特殊字符**：确保特殊字符正确转义

---

## 8. 端口被占用

### 问题描述

```
java.net.BindException: Address already in use: bind
```

### 解决方案

#### 方案 1: 修改端口

在 `application.yml` 中更改端口：

```yaml
server:
  port: 8081
```

#### 方案 2: 释放端口

Windows:
```cmd
netstat -ano | findstr :8080
taskkill /PID <进程ID> /F
```

Linux/Mac:
```bash
lsof -i :8080
kill -9 <进程ID>
```

---

## 9. 依赖冲突

### 问题描述

```
ClassNotFoundException 或版本冲突错误
```

### 解决方案

1. **清理依赖**：

```bash
mvn clean
mvn dependency:purge-local-repository
mvn package
```

2. **查看依赖树**：

```bash
mvn dependency:tree
```

3. **排除冲突依赖**：在 pom.xml 中使用 `<exclusions>`

---

## 10. 调试技巧

### 启用 DEBUG 日志

在 `application.yml` 中：

```yaml
logging:
  level:
    com.datamerge: DEBUG
    org.springframework.web: DEBUG
```

### 查看 Spark 执行计划

```java
dataset.explain();  // 查看逻辑和物理执行计划
```

### 使用 Postman 测试

导入以下 cURL 命令到 Postman：

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d @examples/query-example-http.json
```

---

## 常用命令

### 查看日志

```bash
# 实时查看日志
tail -f logs/application.log

# 查看错误日志
grep ERROR logs/application.log
```

### 性能分析

```bash
# 查看 JVM 内存使用
jmap -heap <PID>

# 查看线程状态
jstack <PID>
```

### 健康检查

```bash
# 检查应用状态
curl http://localhost:8080/api/health

# 检查端口监听
netstat -an | grep 8080
```

---

## 获取帮助

如果以上方案都无法解决问题：

1. 查看完整的错误堆栈信息
2. 检查 `application.yml` 配置
3. 验证数据源连接性
4. 查看相关日志文件
5. 提交 Issue 并附上详细的错误信息

---

## 更新日志

- 2024-11-07: 添加日志框架冲突解决方案
- 2024-11-07: 初始版本创建

