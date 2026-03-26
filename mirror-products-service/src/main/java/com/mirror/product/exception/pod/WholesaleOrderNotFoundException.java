package com.mirror.product.exception.pod;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class WholesaleOrderNotFoundException extends RuntimeException {

    public WholesaleOrderNotFoundException(String message) {
        super(message);
    }

    public WholesaleOrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
