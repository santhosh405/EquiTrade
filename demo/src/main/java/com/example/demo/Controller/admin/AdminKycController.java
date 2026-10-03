package com.example.demo.Controller.admin;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.ApiResponse;
import com.example.demo.model.KycDocument;
import com.example.demo.model.KycTimeline;
import com.example.demo.model.User;
import com.example.demo.model.enums.KycStatus;
import com.example.demo.repository.KycDocumentRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuditLogService;
import com.example.demo.service.KycService;

@RestController
@RequestMapping({"/admin/kyc", "/api/admin/kyc"})
public class AdminKycController {

    @Autowired
    private KycDocumentRepository kycDocumentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KycService kycService;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'KYC_OFFICER', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<List<User>> getPendingKycUsers() {
        List<User> list = userRepository.findByKycStatus(KycStatus.PENDING);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'KYC_OFFICER', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<List<KycDocument>> getAllDocuments() {
        return ResponseEntity.ok(kycDocumentRepository.findAll());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'KYC_OFFICER', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<Map<String, Object>> getUserKycDetails(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        List<KycDocument> documents = kycService.getDocumentsForUser(userId);
        List<KycTimeline> timeline = kycService.getTimelineForUser(userId);

        Map<String, Object> map = new HashMap<>();
        map.put("user", user);
        map.put("documents", documents);
        map.put("timeline", timeline);

        return ResponseEntity.ok(map);
    }

    @PostMapping("/document/{docId}/review")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'KYC_OFFICER', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse> reviewDocument(
            @PathVariable Long docId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String status = body.get("status"); // VERIFIED, REJECTED, RESUBMISSION_REQUESTED
        String remarks = body.get("remarks");

        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Status is required"));
        }

        kycService.reviewDocument(docId, status, remarks, request);
        return ResponseEntity.ok(new ApiResponse(true, "Document review updated to " + status));
    }

    @PostMapping("/user/{userId}/update-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'KYC_OFFICER', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse> updateUserKycStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String statusStr = body.get("status"); // APPROVED, REJECTED, RESUBMISSION_REQUESTED
        String remarks = body.get("remarks");

        if (statusStr == null || statusStr.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Status is required"));
        }

        KycStatus newStatus = KycStatus.valueOf(statusStr.toUpperCase());
        kycService.updateUserKycStatus(userId, newStatus, remarks, request);

        return ResponseEntity.ok(new ApiResponse(true, "User KYC status updated to " + newStatus.name()));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'KYC_OFFICER')")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("userId") Long userId,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "documentNumber", required = false) String documentNumber,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        try {
            KycDocument doc = kycService.uploadDocument(userId, documentType, documentNumber, file, request);
            return ResponseEntity.ok(doc);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error saving document: " + e.getMessage()));
        }
    }

    @PostMapping("/simulate-pan-verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'KYC_OFFICER')")
    public ResponseEntity<Map<String, Object>> simulatePanVerify(@RequestBody Map<String, String> body) {
        String pan = body.get("panNumber");
        boolean valid = kycService.validatePan(pan);
        String masked = kycService.maskDocumentNumber("PAN", pan);

        Map<String, Object> resp = new HashMap<>();
        resp.put("validFormat", valid);
        resp.put("maskedNumber", masked);
        resp.put("status", valid ? "VERIFIED_ACTIVE" : "INVALID_FORMAT");
        resp.put("message", valid ? "PAN format verified against NSDL structure" : "Invalid PAN format. Must match 5 letters, 4 digits, 1 letter.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/simulate-aadhaar-verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'KYC_OFFICER')")
    public ResponseEntity<Map<String, Object>> simulateAadhaarVerify(@RequestBody Map<String, String> body) {
        String aadhaar = body.get("aadhaarNumber");
        boolean valid = kycService.validateAadhaar(aadhaar);
        String masked = kycService.maskDocumentNumber("AADHAAR", aadhaar);

        Map<String, Object> resp = new HashMap<>();
        resp.put("validFormat", valid);
        resp.put("maskedNumber", masked);
        resp.put("status", valid ? "VERIFIED_ACTIVE" : "INVALID_FORMAT");
        resp.put("message", valid ? "Aadhaar format verified via UIDAI mock engine. Masked record stored." : "Invalid Aadhaar. Must be exactly 12 digits.");
        return ResponseEntity.ok(resp);
    }
}
