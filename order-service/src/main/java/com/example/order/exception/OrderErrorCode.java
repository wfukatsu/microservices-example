package com.example.order.exception;

/**
 * Error codes for order processing operations
 */
public enum OrderErrorCode {
    INVENTORY_UNAVAILABLE("Requested inventory is not available"),
    PAYMENT_DECLINED("Payment was declined"),
    SHIPPING_FAILED("Shipping arrangement failed"),
    INVALID_REQUEST("Invalid request data"),
    ORDER_NOT_FOUND("Order not found"),
    TRANSACTION_CONFLICT("Database transaction conflict"),
    SYSTEM_ERROR("Internal system error"),
    UNAUTHORIZED("Unauthorized access"),
    VALIDATION_FAILED("Request validation failed");
    
    private final String description;
    
    OrderErrorCode(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}