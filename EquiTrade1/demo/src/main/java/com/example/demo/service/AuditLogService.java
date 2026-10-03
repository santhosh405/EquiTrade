package com.example.demo.service;

import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.AuditLog;
import com.example.demo.model.User;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.security.SecurityUtil;

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SecurityUtil securityUtil;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logAction(String action, String targetType, String targetId,
                              String beforeValues, String afterValues, String reason,
                              HttpServletRequest request) {
        User admin = securityUtil.getCurrentUser();
        Long adminId = admin != null ? admin.getId() : null;
        String adminEmail = admin != null ? admin.getEmail() : "SYSTEM";
        String role = admin != null ? admin.getRole() : "SYSTEM";

        String ipAddress = "127.0.0.1";
        if (request != null) {
            String forwarded = request.getHeader("X-Forwarded-For");
            ipAddress = (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        }

        AuditLog log = new AuditLog(adminId, adminEmail, role, action, targetType, targetId, beforeValues, afterValues, reason, ipAddress);
        AuditLog saved = auditLogRepository.save(log);
        logger.info("[AUDIT] Admin: {} | Role: {} | Action: {} | Target: {}:{} | Reason: {}",
                adminEmail, role, action, targetType, targetId, reason);
        return saved;
    }
}
