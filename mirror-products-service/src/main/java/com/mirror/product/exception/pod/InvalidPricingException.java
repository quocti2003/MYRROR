package com.mirror.product.exception.pod;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPricingException extends RuntimeException {

    public InvalidPricingException(String message) {
        super(message);
    }

    public InvalidPricingException(String message, Throwable cause) {
        super(message, cause);
    }
}
