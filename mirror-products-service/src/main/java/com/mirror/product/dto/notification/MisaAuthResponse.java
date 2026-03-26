package com.mirror.product.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MisaAuthResponse {

    @JsonProperty("Code")
    private Integer code;

    @JsonProperty("Success")
    private Boolean success;

    @JsonProperty("Data")
    private AuthData data;

    @JsonProperty("ErrorMessage")
    private String errorMessage;

    @JsonProperty("ErrorType")
    private Integer errorType;

    @JsonProperty("Environment")
    private String environment;

    @Data
    public static class AuthData {
        @JsonProperty("AccessToken")
        private String accessToken;

        @JsonProperty("CompanyCode")
        private String companyCode;

        @JsonProperty("Environment")
        private String environment;

        @JsonProperty("Domain")
        private String domain;

        @JsonProperty("AppID")
        private String appId;
    }
}
