package com.example.order.entity;

public enum OrderStatus {
    PENDING("PENDING"),
    CONFIRMED("CONFIRMED"),
    INVENTORY_RESERVED("INVENTORY_RESERVED"),
    PAYMENT_COMPLETED("PAYMENT_COMPLETED"),
    SHIPPED("SHIPPED"),
    DELIVERED("DELIVERED"),
    CANCELLED("CANCELLED"),
    FAILED("FAILED");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static OrderStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        
        for (OrderStatus status : OrderStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        
        throw new IllegalArgumentException("Unknown OrderStatus: " + value);
    }

    public boolean canTransitionTo(OrderStatus newStatus) {
        if (newStatus == null) {
            return false;
        }

        switch (this) {
            case PENDING:
                return newStatus == CONFIRMED || newStatus == CANCELLED || newStatus == FAILED;
            case CONFIRMED:
                return newStatus == INVENTORY_RESERVED || newStatus == CANCELLED || newStatus == FAILED;
            case INVENTORY_RESERVED:
                return newStatus == PAYMENT_COMPLETED || newStatus == CANCELLED || newStatus == FAILED;
            case PAYMENT_COMPLETED:
                return newStatus == SHIPPED || newStatus == CANCELLED || newStatus == FAILED;
            case SHIPPED:
                return newStatus == DELIVERED || newStatus == FAILED;
            case DELIVERED:
                return false; // Terminal state
            case CANCELLED:
                return false; // Terminal state
            case FAILED:
                return newStatus == PENDING; // Allow retry
            default:
                return false;
        }
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }

    public boolean isFailure() {
        return this == FAILED || this == CANCELLED;
    }

    public boolean isSuccess() {
        return this == DELIVERED;
    }
}