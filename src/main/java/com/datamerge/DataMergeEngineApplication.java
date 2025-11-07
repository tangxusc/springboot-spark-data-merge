package com.datamerge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 数据聚合引擎主程序
 */
@SpringBootApplication
@EnableFeignClients
public class DataMergeEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataMergeEngineApplication.class, args);
    }
}

