package com.datamerge.exception;

import com.datamerge.model.QueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<QueryResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Illegal argument: {}", e.getMessage());
        return ResponseEntity
            .badRequest()
            .body(QueryResponse.error("Invalid request: " + e.getMessage()));
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<QueryResponse> handleRuntimeException(RuntimeException e) {
        log.error("Runtime exception occurred", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(QueryResponse.error("Internal error: " + e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<QueryResponse> handleException(Exception e) {
        log.error("Unexpected exception occurred", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(QueryResponse.error("Unexpected error: " + e.getMessage()));
    }
}

