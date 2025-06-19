package com.example.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class PaymentProviderException extends RuntimeException {
    public PaymentProviderException(String message) {
        super(message);
    }
    
    public PaymentProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}