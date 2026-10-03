package com.example.demo.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "regulatory_communications")
public class RegulatoryCommunication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String regulator; // SEBI, RBI, FIU_IND

    @Column(nullable = false)
    private String referenceNumber;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String direction; // INBOUND, OUTBOUND

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, COMPLIED, CLOSED

    @Column(length = 2000)
    private String details;

    private String recordedByAdminEmail;

    private LocalDateTime timestamp = LocalDateTime.now();

    public RegulatoryCommunication() {
    }

    public RegulatoryCommunication(String regulator, String referenceNumber, String subject,
                                   String direction, String status, String details, String recordedByAdminEmail) {
        this.regulator = regulator;
        this.referenceNumber = referenceNumber;
        this.subject = subject;
        this.direction = direction;
        this.status = status;
        this.details = details;
        this.recordedByAdminEmail = recordedByAdminEmail;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRegulator() {
        return regulator;
    }

    public void setRegulator(String regulator) {
        this.regulator = regulator;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getRecordedByAdminEmail() {
        return recordedByAdminEmail;
    }

    public void setRecordedByAdminEmail(String recordedByAdminEmail) {
        this.recordedByAdminEmail = recordedByAdminEmail;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
