package com.example.demo.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "kyc_documents")
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String documentType; // PAN, AADHAAR_FRONT, AADHAAR_BACK, PASSPORT

    private String fileName;

    private String fileType;

    private long fileSize;

    private String storagePath;

    private String maskedDocumentNumber; // e.g. XXXX-XXXX-1234

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, VERIFIED, REJECTED

    private String remarks;

    private LocalDateTime uploadedAt = LocalDateTime.now();

    private LocalDateTime verifiedAt;

    private String verifiedByAdminEmail;

    public KycDocument() {
    }

    public KycDocument(User user, String documentType, String fileName, String fileType,
                       long fileSize, String storagePath, String maskedDocumentNumber) {
        this.user = user;
        this.documentType = documentType;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.storagePath = storagePath;
        this.maskedDocumentNumber = maskedDocumentNumber;
        this.status = "PENDING";
        this.uploadedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getMaskedDocumentNumber() {
        return maskedDocumentNumber;
    }

    public void setMaskedDocumentNumber(String maskedDocumentNumber) {
        this.maskedDocumentNumber = maskedDocumentNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getVerifiedByAdminEmail() {
        return verifiedByAdminEmail;
    }

    public void setVerifiedByAdminEmail(String verifiedByAdminEmail) {
        this.verifiedByAdminEmail = verifiedByAdminEmail;
    }
}
