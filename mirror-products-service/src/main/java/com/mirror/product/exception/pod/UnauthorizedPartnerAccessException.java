package com.mirror.product.exception.pod;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedPartnerAccessException extends RuntimeException {

    public UnauthorizedPartnerAccessException(String message) {
        super(message);
    }

    public UnauthorizedPartnerAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
