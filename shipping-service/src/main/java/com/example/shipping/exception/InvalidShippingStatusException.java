package com.example.shipping.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidShippingStatusException extends RuntimeException {
    public InvalidShippingStatusException(String message) {
        super(message);
    }
    
    public InvalidShippingStatusException(String message, Throwable cause) {
        super(message, cause);
    }
}