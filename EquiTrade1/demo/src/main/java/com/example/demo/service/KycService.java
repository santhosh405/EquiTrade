package com.example.demo.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.KycDocument;
import com.example.demo.model.KycTimeline;
import com.example.demo.model.User;
import com.example.demo.model.enums.KycStatus;
import com.example.demo.repository.KycDocumentRepository;
import com.example.demo.repository.KycTimelineRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.SecurityUtil;

@Service
public class KycService {

    private static final String STORAGE_DIR = System.getProperty("user.dir") + File.separator + "equitrade_kyc_storage";
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("^\\d{12}$");

    @Autowired
    private KycDocumentRepository kycDocumentRepository;

    @Autowired
    private KycTimelineRepository kycTimelineRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private AuditLogService auditLogService;

    public KycService() {
        File dir = new File(STORAGE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Transactional
    public KycDocument uploadDocument(Long userId, String documentType, String documentNumber,
                                      MultipartFile file, HttpServletRequest request) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/pdf") &&
                                    !contentType.equals("image/jpeg") &&
                                    !contentType.equals("image/png"))) {
            throw new IllegalArgumentException("Only PDF, JPEG, and PNG files are allowed");
        }

        String masked = maskDocumentNumber(documentType, documentNumber);

        // Store outside web root
        String storedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path targetPath = Paths.get(STORAGE_DIR, storedFileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        KycDocument doc = new KycDocument(user, documentType, file.getOriginalFilename(),
                contentType, file.getSize(), targetPath.toString(), masked);
        KycDocument saved = kycDocumentRepository.save(doc);

        // Update user KYC status to PENDING if not already APPROVED
        if (user.getKycStatus() != KycStatus.APPROVED) {
            KycStatus prev = user.getKycStatus();
            user.setKycStatus(KycStatus.PENDING);
            userRepository.save(user);

            kycTimelineRepository.save(new KycTimeline(user, null, "USER", "SUBMITTED",
                    prev.name(), KycStatus.PENDING.name(), "Uploaded " + documentType));
        }

        auditLogService.logAction("UPLOAD_KYC_DOCUMENT", "USER", user.getId().toString(),
                null, documentType + " (" + masked + ")", "User uploaded KYC document", request);

        return saved;
    }

    public boolean validatePan(String pan) {
        if (pan == null) return false;
        return PAN_PATTERN.matcher(pan.trim().toUpperCase()).matches();
    }

    public boolean validateAadhaar(String aadhaar) {
        if (aadhaar == null) return false;
        String clean = aadhaar.replaceAll("[\\s-]", "");
        return AADHAAR_PATTERN.matcher(clean).matches();
    }

    public String maskDocumentNumber(String documentType, String number) {
        if (number == null || number.isBlank()) return "NOT_PROVIDED";
        String clean = number.replaceAll("[\\s-]", "").trim();
        if ("AADHAAR".equalsIgnoreCase(documentType) || clean.length() == 12) {
            return "XXXX-XXXX-" + clean.substring(clean.length() - 4);
        }
        if ("PAN".equalsIgnoreCase(documentType) || clean.length() == 10) {
            return clean.substring(0, 2) + "XXXXX" + clean.substring(clean.length() - 3);
        }
        return "XXXX-" + (clean.length() > 4 ? clean.substring(clean.length() - 4) : clean);
    }

    @Transactional
    public void reviewDocument(Long docId, String status, String remarks, HttpServletRequest request) {
        KycDocument doc = kycDocumentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + docId));

        User admin = securityUtil.getCurrentUser();
        String adminEmail = admin != null ? admin.getEmail() : "SYSTEM";
        Long adminId = admin != null ? admin.getId() : null;

        String before = doc.getStatus();
        doc.setStatus(status.toUpperCase());
        doc.setRemarks(remarks);
        doc.setVerifiedAt(LocalDateTime.now());
        doc.setVerifiedByAdminEmail(adminEmail);
        kycDocumentRepository.save(doc);

        auditLogService.logAction("REVIEW_KYC_DOCUMENT", "KYC_DOC", docId.toString(),
                before, status.toUpperCase(), remarks, request);
    }

    @Transactional
    public void updateUserKycStatus(Long userId, KycStatus newStatus, String remarks, HttpServletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        User admin = securityUtil.getCurrentUser();
        String adminEmail = admin != null ? admin.getEmail() : "SYSTEM";
        Long adminId = admin != null ? admin.getId() : null;

        KycStatus prev = user.getKycStatus();
        user.setKycStatus(newStatus);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        kycTimelineRepository.save(new KycTimeline(user, adminId, adminEmail,
                newStatus.name(), prev.name(), newStatus.name(), remarks));

        auditLogService.logAction("UPDATE_USER_KYC_STATUS", "USER", userId.toString(),
                prev.name(), newStatus.name(), remarks, request);
    }

    public List<KycDocument> getDocumentsForUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return List.of();
        return kycDocumentRepository.findByUserOrderByUploadedAtDesc(user);
    }

    public List<KycTimeline> getTimelineForUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return List.of();
        return kycTimelineRepository.findByUserOrderByTimestampDesc(user);
    }
}
