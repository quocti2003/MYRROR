package com.mirror.product.exception.pod;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPartnerStatusTransitionException extends RuntimeException {

    public InvalidPartnerStatusTransitionException(String message) {
        super(message);
    }

    public InvalidPartnerStatusTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
