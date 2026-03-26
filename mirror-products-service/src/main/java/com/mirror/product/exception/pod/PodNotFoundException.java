package com.mirror.product.exception.pod;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PodNotFoundException extends RuntimeException {

    public PodNotFoundException(String message) {
        super(message);
    }

    public PodNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
