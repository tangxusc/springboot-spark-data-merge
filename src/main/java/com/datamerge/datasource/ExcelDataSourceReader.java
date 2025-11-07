package com.datamerge.datasource;

import com.datamerge.model.ExcelDataSourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 文件数据源读取器
 */
@Slf4j
@Component
public class ExcelDataSourceReader {
    
    /**
     * 读取 Excel 文件数据源
     */
    public Dataset<Row> read(SparkSession spark, ExcelDataSourceConfig config) {
        try {
            log.info("Reading Excel data source: {}", config.getPath());
            
            try (FileInputStream fis = new FileInputStream(config.getPath());
                 Workbook workbook = WorkbookFactory.create(fis)) {
                
                Sheet sheet = workbook.getSheet(config.getSheet());
                if (sheet == null) {
                    sheet = workbook.getSheetAt(0);
                }
                
                // 读取表头
                List<String> headers = new ArrayList<>();
                org.apache.poi.ss.usermodel.Row headerRow = sheet.getRow(config.getStartRow());
                
                if (config.isHasHeader() && headerRow != null) {
                    for (Cell cell : headerRow) {
                        headers.add(getCellValueAsString(cell));
                    }
                } else {
                    // 如果没有表头，生成默认列名
                    int columnCount = headerRow != null ? headerRow.getLastCellNum() : 0;
                    for (int i = 0; i < columnCount; i++) {
                        headers.add("column_" + i);
                    }
                }
                
                // 构建 Schema
                StructField[] fields = new StructField[headers.size()];
                for (int i = 0; i < headers.size(); i++) {
                    fields[i] = DataTypes.createStructField(headers.get(i), DataTypes.StringType, true);
                }
                StructType schema = DataTypes.createStructType(fields);
                
                // 读取数据行
                List<Row> rows = new ArrayList<>();
                int startDataRow = config.isHasHeader() ? config.getStartRow() + 1 : config.getStartRow();
                
                for (int i = startDataRow; i <= sheet.getLastRowNum(); i++) {
                    org.apache.poi.ss.usermodel.Row excelRow = sheet.getRow(i);
                    if (excelRow == null) {
                        continue;
                    }
                    
                    Object[] values = new Object[headers.size()];
                    for (int j = 0; j < headers.size(); j++) {
                        Cell cell = excelRow.getCell(j);
                        values[j] = cell != null ? getCellValueAsString(cell) : null;
                    }
                    rows.add(RowFactory.create(values));
                }
                
                // 创建 Dataset
                Dataset<Row> dataset = spark.createDataFrame(rows, schema);
                
                log.info("Excel data source loaded successfully: {} rows", dataset.count());
                return dataset;
            }
            
        } catch (IOException e) {
            log.error("Failed to read Excel data source: {}", config.getPath(), e);
            throw new RuntimeException("Failed to read Excel data source: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取单元格值为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}

