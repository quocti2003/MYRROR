package com.mirror.product.printing.exception;

import com.mirror.product.printing.dto.PrintingApiResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(basePackages = "com.mirror.product.printing")
@Order(1)
public class PrintingExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<PrintingApiResponse> handleMissingParams(MissingServletRequestParameterException ex) {
        String message = "Parameter '" + ex.getParameterName() + "' is required";
        return ResponseEntity.badRequest()
            .body(PrintingApiResponse.error("Missing parameter", message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<PrintingApiResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Parameter '" + ex.getName() + "' has invalid type";
        return ResponseEntity.badRequest()
            .body(PrintingApiResponse.error("Invalid parameter type", message));
    }
}
