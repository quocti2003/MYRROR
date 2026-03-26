package com.mirror.product.service.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mirror.product.config.notification.TurnstileProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
public class TurnstileValidator {

    private static final Logger log = LoggerFactory.getLogger(TurnstileValidator.class);
    private static final String VERIFY_PATH = "/siteverify";

    private final TurnstileProperties properties;
    private final RestClient restClient;

    public TurnstileValidator(TurnstileProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl("https://challenges.cloudflare.com/turnstile/v0")
                .build();
    }

    public void validate(String token, String remoteIp) {
        if (!properties.isEnabled()) {
            return;
        }
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Captcha verification is required.");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", properties.getSecret());
        body.add("response", token);
        if (remoteIp != null && !remoteIp.isBlank()) {
            body.add("remoteip", remoteIp);
        }

        TurnstileResponse response;
        try {
            response = restClient.post()
                    .uri(VERIFY_PATH)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(TurnstileResponse.class);
        } catch (Exception ex) {
            log.warn("Failed to verify Turnstile token", ex);
            throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "Verification service is unavailable. Please retry in a moment."
            );
        }

        if (response == null || !response.success()) {
            log.info("Turnstile verification failed: errors={}", response != null ? response.errorCodes() : List.of());
            throw new ResponseStatusException(BAD_REQUEST, "Verification failed. Please try again.");
        }
    }

    private record TurnstileResponse(boolean success, @JsonProperty("error-codes") List<String> errorCodes,
                                     @JsonProperty("challenge_ts") String challengeTs,
                                     String hostname)
    {
    }
}
