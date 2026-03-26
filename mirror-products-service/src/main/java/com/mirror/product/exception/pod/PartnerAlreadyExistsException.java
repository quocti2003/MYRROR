package com.mirror.product.exception.pod;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PartnerAlreadyExistsException extends RuntimeException {

    public PartnerAlreadyExistsException(String message) {
        super(message);
    }

    public PartnerAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
