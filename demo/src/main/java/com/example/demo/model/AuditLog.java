package com.example.demo.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long adminId;

    private String adminEmail;

    private String role;

    @Column(nullable = false)
    private String action;

    private String targetType;

    private String targetId;

    @Lob
    @Column(length = 4000)
    private String beforeValues;

    @Lob
    @Column(length = 4000)
    private String afterValues;

    @Column(length = 1000)
    private String reason;

    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public AuditLog() {
    }

    public AuditLog(Long adminId, String adminEmail, String role, String action, String targetType,
                    String targetId, String beforeValues, String afterValues, String reason, String ipAddress) {
        this.adminId = adminId;
        this.adminEmail = adminEmail;
        this.role = role;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.beforeValues = beforeValues;
        this.afterValues = afterValues;
        this.reason = reason;
        this.ipAddress = ipAddress;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getBeforeValues() {
        return beforeValues;
    }

    public void setBeforeValues(String beforeValues) {
        this.beforeValues = beforeValues;
    }

    public String getAfterValues() {
        return afterValues;
    }

    public void setAfterValues(String afterValues) {
        this.afterValues = afterValues;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
