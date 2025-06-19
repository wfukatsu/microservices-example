package com.example.shipping.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class CarrierIntegrationException extends RuntimeException {
    public CarrierIntegrationException(String message) {
        super(message);
    }
    
    public CarrierIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}