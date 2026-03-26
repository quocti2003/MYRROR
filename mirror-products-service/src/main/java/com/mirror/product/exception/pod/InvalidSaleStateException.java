package com.mirror.product.exception.pod;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidSaleStateException extends RuntimeException {

    public InvalidSaleStateException(String message) {
        super(message);
    }

    public InvalidSaleStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
