package com.mirror.product.exception.pod;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PartnerSaleNotFoundException extends RuntimeException {

    public PartnerSaleNotFoundException(String message) {
        super(message);
    }

    public PartnerSaleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
