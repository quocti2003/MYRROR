package com.mirror.product.controller.notification;

import com.mirror.product.dto.notification.event.NotificationEvent;
import com.mirror.product.dto.notification.request.AppointmentNotificationRequest;
import com.mirror.product.dto.notification.request.EmailSendRequest;
import com.mirror.product.enums.EmailType;
import com.mirror.product.service.notification.DirectEmailService;
import com.mirror.product.service.notification.NotificationDispatcher;
import com.mirror.product.service.notification.SubmissionRateLimiter;
import com.mirror.product.service.notification.TurnstileValidator;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/internal/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationDispatcher dispatcher;
    private final DirectEmailService directEmailService;
    private final TurnstileValidator turnstileValidator;
    private final SubmissionRateLimiter submissionRateLimiter;

    public NotificationController(NotificationDispatcher dispatcher,
                                  DirectEmailService directEmailService,
                                  TurnstileValidator turnstileValidator,
                                  SubmissionRateLimiter submissionRateLimiter) {
        this.dispatcher = dispatcher;
        this.directEmailService = directEmailService;
        this.turnstileValidator = turnstileValidator;
        this.submissionRateLimiter = submissionRateLimiter;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("notification-service is running (embedded in products-service)");
    }

    @PostMapping
    public ResponseEntity<Void> trigger(@Valid @RequestBody NotificationEvent event) {
        dispatcher.dispatch(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/email")
    public ResponseEntity<Void> sendEmail(@Valid @RequestBody EmailSendRequest request,
                                          HttpServletRequest servletRequest) {
        String clientIp = resolveClientIp(servletRequest);
        turnstileValidator.validate(request.getCaptchaToken(), clientIp);
        submissionRateLimiter.validate(clientIp, request.safeRecipients());
        directEmailService.send(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Internal endpoint for sending appointment confirmation emails.
     * This endpoint is called by the products service and does not require captcha validation.
     */
    @PostMapping("/appointment")
    public ResponseEntity<Void> sendAppointmentConfirmation(@Valid @RequestBody AppointmentNotificationRequest request) {
        log.info("Sending appointment confirmation email to {}", request.getCustomerEmail());

        Map<String, Object> model = new HashMap<>();
        model.put("customerTitle", request.getCustomerTitle());
        model.put("customerName", request.getCustomerName());
        model.put("customerEmail", request.getCustomerEmail());
        model.put("service", request.getService());
        model.put("venueName", request.getVenueName());
        model.put("venueAddress", request.getVenueAddress());
        model.put("appointmentDate", request.getAppointmentDate());
        model.put("appointmentTime", request.getAppointmentTime());

        EmailSendRequest emailRequest = new EmailSendRequest();
        emailRequest.setRecipients(Set.of(request.getCustomerEmail()));
        emailRequest.setEmailType(EmailType.APPOINTMENT_CONFIRMATION);
        emailRequest.setModel(model);

        directEmailService.send(emailRequest);

        log.info("Appointment confirmation email sent to {}", request.getCustomerEmail());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        for (String header : IP_HEADER_CANDIDATES) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) {
                return value.split(",", 2)[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private static final String[] IP_HEADER_CANDIDATES = {
            "CF-Connecting-IP",
            "X-Forwarded-For",
            "X-Real-IP",
            "X-Client-IP",
            "X-Forwarded",
            "Forwarded-For",
            "Forwarded"
    };
}
