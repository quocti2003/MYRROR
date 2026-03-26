package com.mirror.product.service.user;

import com.mirror.product.entity.user.AuditLog;
import com.mirror.product.repository.user.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    
    @Transactional
    public void logAuthenticationEvent(String username, String action, String resource, 
                                     String ipAddress, String userAgent, 
                                     boolean success, String errorMessage) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUsername(username);
            auditLog.setAction(action);
            auditLog.setResource(resource);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setSuccess(success);
            auditLog.setErrorMessage(errorMessage);
            auditLog.setTimestamp(LocalDateTime.now());
            
            auditLogRepository.save(auditLog);
            
            log.info("Audit event logged: {} - {} - Success: {}", username, action, success);
        } catch (Exception e) {
            log.error("Failed to log audit event for user: {} - action: {}", username, action, e);
        }
    }
    
    @Transactional
    public void logUserEvent(String username, String action, String resource, 
                           String ipAddress, String userAgent, 
                           boolean success, String details, String errorMessage) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUsername(username);
            auditLog.setAction(action);
            auditLog.setResource(resource);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setSuccess(success);
            auditLog.setDetails(details);
            auditLog.setErrorMessage(errorMessage);
            auditLog.setTimestamp(LocalDateTime.now());
            
            auditLogRepository.save(auditLog);
            
            log.info("User event logged: {} - {} - Success: {}", username, action, success);
        } catch (Exception e) {
            log.error("Failed to log user event for user: {} - action: {}", username, action, e);
        }
    }
}