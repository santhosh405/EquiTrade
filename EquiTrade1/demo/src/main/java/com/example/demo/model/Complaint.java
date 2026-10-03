package com.example.demo.model;

import java.time.LocalDateTime;
import com.example.demo.model.enums.ComplaintStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String userEmail;

    @Column(nullable = false)
    private String subject;

    @Column(length = 2000, nullable = false)
    private String description;

    @Column(nullable = false)
    private String category; // TRADING, KYC, FUNDS, ACCOUNT

    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, URGENT

    @Enumerated(EnumType.STRING)
    private ComplaintStatus status = ComplaintStatus.OPEN;

    private String assignedToAdminEmail;

    @Column(length = 2000)
    private String resolutionNotes;

    private LocalDateTime slaDueAt = LocalDateTime.now().plusHours(48); // 48-hour default SLA

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;

    public Complaint() {
    }

    public Complaint(Long userId, String userEmail, String subject, String description,
                     String category, String priority, int slaHours) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.subject = subject;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.status = ComplaintStatus.OPEN;
        this.createdAt = LocalDateTime.now();
        this.slaDueAt = LocalDateTime.now().plusHours(slaHours > 0 ? slaHours : 48);
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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public String getAssignedToAdminEmail() {
        return assignedToAdminEmail;
    }

    public void setAssignedToAdminEmail(String assignedToAdminEmail) {
        this.assignedToAdminEmail = assignedToAdminEmail;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public LocalDateTime getSlaDueAt() {
        return slaDueAt;
    }

    public void setSlaDueAt(LocalDateTime slaDueAt) {
        this.slaDueAt = slaDueAt;
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
