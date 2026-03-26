package com.mirror.product.printing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrintingApiResponse {
    private boolean success;
    private String error;
    private String message;

    // For databases endpoint
    private List<String> databases;

    // For tables endpoint
    private String database;
    private List<String> tables;

    // For columns endpoint
    private String table;
    private List<ColumnInfo> columns;

    // For data endpoint
    private List<String> columnNames;
    private Integer rowCount;
    private List<Map<String, Object>> data;

    // For product endpoint
    private String sku;

    public static PrintingApiResponse success() {
        PrintingApiResponse response = new PrintingApiResponse();
        response.setSuccess(true);
        return response;
    }

    public static PrintingApiResponse error(String error) {
        PrintingApiResponse response = new PrintingApiResponse();
        response.setSuccess(false);
        response.setError(error);
        return response;
    }

    public static PrintingApiResponse error(String error, String message) {
        PrintingApiResponse response = new PrintingApiResponse();
        response.setSuccess(false);
        response.setError(error);
        response.setMessage(message);
        return response;
    }
}
