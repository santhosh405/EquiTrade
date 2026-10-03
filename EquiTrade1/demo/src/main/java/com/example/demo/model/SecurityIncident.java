package com.example.demo.model;

import java.time.LocalDateTime;
import com.example.demo.model.enums.SecurityIncidentStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "security_incidents")
public class SecurityIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String incidentType; // ACCOUNT_TAKEOVER, SUSPICIOUS_PAYMENT, UNUSUAL_TRANSACTION, MULTIPLE_DEVICE_COLLISION, IMPOSSIBLE_LOCATION

    private String targetType; // USER, TRANSACTION, LOGIN, IP

    private String targetId;

    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @Enumerated(EnumType.STRING)
    private SecurityIncidentStatus status = SecurityIncidentStatus.OPEN;

    @Column(length = 2000, nullable = false)
    private String details;

    @Column(length = 2000)
    private String investigationNotes;

    private String resolution;

    private String assignedToAdminEmail;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime closedAt;

    public SecurityIncident() {
    }

    public SecurityIncident(String incidentType, String targetType, String targetId, String severity, String details) {
        this.incidentType = incidentType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.severity = severity;
        this.details = details;
        this.status = SecurityIncidentStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
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

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public SecurityIncidentStatus getStatus() {
        return status;
    }

    public void setStatus(SecurityIncidentStatus status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getInvestigationNotes() {
        return investigationNotes;
    }

    public void setInvestigationNotes(String investigationNotes) {
        this.investigationNotes = investigationNotes;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getAssignedToAdminEmail() {
        return assignedToAdminEmail;
    }

    public void setAssignedToAdminEmail(String assignedToAdminEmail) {
        this.assignedToAdminEmail = assignedToAdminEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
}
