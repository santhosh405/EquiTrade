package com.example.demo.model;

import java.time.LocalDateTime;
import com.example.demo.model.enums.RiskAlertSeverity;
import com.example.demo.model.enums.RiskAlertStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "risk_alerts")
public class RiskAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String userEmail;

    @Column(nullable = false)
    private String alertType; // ABNORMAL_ORDER_SIZE, EXPOSURE_LIMIT_EXCEEDED, UNUSUAL_PRICE_DEVIATION, MARGIN_SHORTFALL, SUSPICIOUS_LOGIN_ATTEMPTS

    @Enumerated(EnumType.STRING)
    private RiskAlertSeverity severity = RiskAlertSeverity.MEDIUM;

    @Column(length = 2000, nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    private RiskAlertStatus status = RiskAlertStatus.OPEN;

    private String actionTaken;

    private String resolvedByAdminEmail;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;

    public RiskAlert() {
    }

    public RiskAlert(Long userId, String userEmail, String alertType, RiskAlertSeverity severity, String description) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.alertType = alertType;
        this.severity = severity;
        this.description = description;
        this.status = RiskAlertStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public RiskAlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(RiskAlertSeverity severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RiskAlertStatus getStatus() {
        return status;
    }

    public void setStatus(RiskAlertStatus status) {
        this.status = status;
    }

    public String getActionTaken() {
        return actionTaken;
    }

    public void setActionTaken(String actionTaken) {
        this.actionTaken = actionTaken;
    }

    public String getResolvedByAdminEmail() {
        return resolvedByAdminEmail;
    }

    public void setResolvedByAdminEmail(String resolvedByAdminEmail) {
        this.resolvedByAdminEmail = resolvedByAdminEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
