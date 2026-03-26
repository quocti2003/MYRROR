package com.mirror.product.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MisaApiResponse<T> {

    @JsonProperty("Code")
    private Integer code;

    @JsonProperty("Success")
    private Boolean success;

    @JsonProperty("Data")
    private List<T> data;

    @JsonProperty("Total")
    private Integer total;

    @JsonProperty("ErrorMessage")
    private String errorMessage;

    @JsonProperty("ErrorType")
    private Integer errorType;

    @JsonProperty("Environment")
    private String environment;
}
