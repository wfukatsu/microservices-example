package com.example.order.exception;

import com.example.order.dto.ErrorResponse;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.exception.transaction.AbortException;
import com.scalar.db.exception.transaction.TransactionException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for order processing errors
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(OrderProcessingException.class)
    public ResponseEntity<ErrorResponse> handleOrderProcessingException(OrderProcessingException e) {
        logger.error("Order processing exception: {} for order: {}", e.getErrorCode(), e.getOrderId(), e);
        
        ErrorResponse error = new ErrorResponse();
        error.setError(e.getErrorCode().name());
        error.setMessage(e.getMessage());
        error.setTimestamp(System.currentTimeMillis());
        
        // Add order-specific information
        Map<String, Object> details = new HashMap<>();
        details.put("orderId", e.getOrderId());
        details.put("errorCode", e.getErrorCode().name());
        error.setDetails(details);
        
        HttpStatus status = mapErrorCodeToHttpStatus(e.getErrorCode());
        return ResponseEntity.status(status).body(error);
    }
    
    @ExceptionHandler(AbortException.class)
    public ResponseEntity<ErrorResponse> handleAbortException(AbortException e) {
        logger.error("Transaction conflict occurred", e);
        
        ErrorResponse error = new ErrorResponse();
        error.setError(OrderErrorCode.TRANSACTION_CONFLICT.name());
        error.setMessage("Transaction conflict occurred. Please try again.");
        error.setTimestamp(System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<ErrorResponse> handleTransactionException(TransactionException e) {
        logger.error("Transaction system error", e);
        
        ErrorResponse error = new ErrorResponse();
        error.setError(OrderErrorCode.SYSTEM_ERROR.name());
        error.setMessage("Transaction system error occurred");
        error.setTimestamp(System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    @ExceptionHandler(ExecutionException.class)
    public ResponseEntity<ErrorResponse> handleExecutionException(ExecutionException e) {
        logger.error("Database execution error", e);
        
        ErrorResponse error = new ErrorResponse();
        error.setError(OrderErrorCode.SYSTEM_ERROR.name());
        error.setMessage("Database operation failed");
        error.setTimestamp(System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        logger.warn("Validation failed for request", e);
        
        ErrorResponse error = new ErrorResponse();
        error.setError(OrderErrorCode.VALIDATION_FAILED.name());
        error.setMessage("Request validation failed");
        error.setTimestamp(System.currentTimeMillis());
        
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        
        Map<String, Object> details = new HashMap<>();
        details.put("fieldErrors", fieldErrors);
        error.setDetails(details);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        logger.warn("Constraint violation", e);
        
        ErrorResponse error = new ErrorResponse();
        error.setError(OrderErrorCode.VALIDATION_FAILED.name());
        error.setMessage("Request validation failed: " + e.getMessage());
        error.setTimestamp(System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("Invalid argument: {}", e.getMessage());
        
        ErrorResponse error = new ErrorResponse();
        error.setError(OrderErrorCode.INVALID_REQUEST.name());
        error.setMessage(e.getMessage());
        error.setTimestamp(System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        logger.error("Unexpected runtime exception", e);
        
        ErrorResponse error = new ErrorResponse();
        error.setError(OrderErrorCode.SYSTEM_ERROR.name());
        error.setMessage("An unexpected error occurred");
        error.setTimestamp(System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    private HttpStatus mapErrorCodeToHttpStatus(OrderErrorCode errorCode) {
        return switch (errorCode) {
            case INVENTORY_UNAVAILABLE, PAYMENT_DECLINED, SHIPPING_FAILED -> HttpStatus.UNPROCESSABLE_ENTITY;
            case INVALID_REQUEST, VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
            case ORDER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case TRANSACTION_CONFLICT -> HttpStatus.CONFLICT;
            case SYSTEM_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}