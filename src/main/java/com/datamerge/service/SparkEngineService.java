package com.datamerge.service;

import com.datamerge.config.SparkConfig;
import com.datamerge.datasource.ExcelDataSourceReader;
import com.datamerge.datasource.HttpDataSourceReader;
import com.datamerge.datasource.JsonDataSourceReader;
import com.datamerge.datasource.MySQLDataSourceReader;
import com.datamerge.model.*;
import com.datamerge.util.ParameterResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spark 引擎服务
 */
@Slf4j
@Service
public class SparkEngineService {
    
    private final SparkConfig sparkConfig;
    private final ParameterResolver parameterResolver;
    private final HttpDataSourceReader httpDataSourceReader;
    private final JsonDataSourceReader jsonDataSourceReader;
    private final ExcelDataSourceReader excelDataSourceReader;
    private final MySQLDataSourceReader mysqlDataSourceReader;
    
    private SparkSession sparkSession;
    
    public SparkEngineService(SparkConfig sparkConfig,
                             ParameterResolver parameterResolver,
                             HttpDataSourceReader httpDataSourceReader,
                             JsonDataSourceReader jsonDataSourceReader,
                             ExcelDataSourceReader excelDataSourceReader,
                             MySQLDataSourceReader mysqlDataSourceReader) {
        this.sparkConfig = sparkConfig;
        this.parameterResolver = parameterResolver;
        this.httpDataSourceReader = httpDataSourceReader;
        this.jsonDataSourceReader = jsonDataSourceReader;
        this.excelDataSourceReader = excelDataSourceReader;
        this.mysqlDataSourceReader = mysqlDataSourceReader;
    }
    
    @PostConstruct
    public void init() {
        log.info("Initializing Spark Session...");
        
        SparkSession.Builder builder = SparkSession.builder()
            .appName(sparkConfig.getAppName())
            .master(sparkConfig.getMaster())
            .config("spark.driver.memory", sparkConfig.getDriverMemory())
            .config("spark.executor.memory", sparkConfig.getExecutorMemory())
            .config("spark.ui.enabled", sparkConfig.isUiEnabled())
            // .config("spark.sql.codegen.wholeStage", "false")
            .config("spark.sql.warehouse.dir", "file:///tmp/spark-warehouse")
            .config("spark.driver.extraJavaOptions", "-Dlog4j.configuration=log4j.properties");
        
        sparkSession = builder.getOrCreate();
        
        // 注意：不要调用 setLogLevel()，因为会导致日志框架冲突
        // 日志级别通过 application.yml 中的 logging.level 配置
        
        log.info("Spark Session initialized successfully");
    }
    
    @PreDestroy
    public void destroy() {
        if (sparkSession != null) {
            log.info("Closing Spark Session...");
            sparkSession.close();
        }
    }
    
    /**
     * 执行查询
     */
    public List<Map<String, Object>> executeQuery(QueryRequest request) {
        try {
            // 1. 创建临时视图
            for (DataSourceConfig config : request.getDataSources()) {
                createTempView(config, request.getParams());
            }
            
            // 2. 执行 SQL 查询
            Dataset<Row> resultDataset = sparkSession.sql(request.getSql());
            resultDataset.show();
            resultDataset.explain();
            
            // 3. 转换结果为 List<Map>
            return convertToList(resultDataset);
            
        } catch (Exception e) {
            log.error("Failed to execute query", e);
            throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建临时视图
     */
    private void createTempView(DataSourceConfig config, Map<String, Object> params) {
        Dataset<Row> dataset = null;
        
        if (config instanceof HttpDataSourceConfig) {
            HttpDataSourceConfig httpConfig = (HttpDataSourceConfig) config;
            // 解析参数
            httpConfig.setUrl(parameterResolver.resolve(httpConfig.getUrl(), params));
            if (httpConfig.getHeaders() != null) {
                httpConfig.setHeaders(parameterResolver.resolveMap(httpConfig.getHeaders(), params));
            }
            dataset = httpDataSourceReader.read(sparkSession, httpConfig);
            
        } else if (config instanceof JsonDataSourceConfig) {
            JsonDataSourceConfig jsonConfig = (JsonDataSourceConfig) config;
            dataset = jsonDataSourceReader.read(sparkSession, jsonConfig);
            
        } else if (config instanceof ExcelDataSourceConfig) {
            ExcelDataSourceConfig excelConfig = (ExcelDataSourceConfig) config;
            dataset = excelDataSourceReader.read(sparkSession, excelConfig);
            
        } else if (config instanceof MySQLDataSourceConfig) {
            MySQLDataSourceConfig mysqlConfig = (MySQLDataSourceConfig) config;
            // 解析参数
            mysqlConfig.setUrl(parameterResolver.resolve(mysqlConfig.getUrl(), params));
            mysqlConfig.setUsername(parameterResolver.resolve(mysqlConfig.getUsername(), params));
            mysqlConfig.setPassword(parameterResolver.resolve(mysqlConfig.getPassword(), params));
            if (mysqlConfig.getTable() != null) {
                mysqlConfig.setTable(parameterResolver.resolve(mysqlConfig.getTable(), params));
            }
            dataset = mysqlDataSourceReader.read(sparkSession, mysqlConfig);
        }
        
        if (dataset != null) {
            dataset.createOrReplaceTempView(config.getName());
            log.info("Created temp view: {}", config.getName());
        }
    }
    
    /**
     * 将 Dataset<Row> 转换为 List<Map<String, Object>>
     */
    private List<Map<String, Object>> convertToList(Dataset<Row> dataset) {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] columns = dataset.columns();
        
        List<Row> rows = dataset.collectAsList();
        for (Row row : rows) {
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < columns.length; i++) {
                //GenericRowWithSchema 转换为 Map<String, Object>
                if (row.isNullAt(i)) {
                    map.put(columns[i], null);
                } else if (row.get(i) instanceof GenericRowWithSchema) {
                    GenericRowWithSchema genericRow = (GenericRowWithSchema) row.get(i);
                    Map<String, Object> structMap = new HashMap<>();
                    String[] fieldNames = genericRow.schema().fieldNames();
                    for (int j = 0; j < genericRow.length(); j++) {
                        structMap.put(fieldNames[j], genericRow.get(j));
                    }
                    map.put(columns[i], structMap);
                } else {
                    map.put(columns[i], row.get(i));
                }
            }
            result.add(map);
        }
        
        return result;
    }
}

