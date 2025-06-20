package com.example.order.exception;

/**
 * Custom exception for order processing errors with specific error codes
 */
public class OrderProcessingException extends Exception {
    
    private final OrderErrorCode errorCode;
    private final String orderId;
    
    public OrderProcessingException(OrderErrorCode errorCode, String orderId, String message) {
        super(message);
        this.errorCode = errorCode;
        this.orderId = orderId;
    }
    
    public OrderProcessingException(OrderErrorCode errorCode, String orderId, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.orderId = orderId;
    }
    
    public OrderErrorCode getErrorCode() {
        return errorCode;
    }
    
    public String getOrderId() {
        return orderId;
    }
}