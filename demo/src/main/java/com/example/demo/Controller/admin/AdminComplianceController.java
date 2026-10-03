package com.example.demo.Controller.admin;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Complaint;
import com.example.demo.model.RegulatoryCommunication;
import com.example.demo.model.User;
import com.example.demo.model.enums.ComplaintStatus;
import com.example.demo.model.enums.KycStatus;
import com.example.demo.model.enums.RiskCategory;
import com.example.demo.repository.ComplaintRepository;
import com.example.demo.repository.RegulatoryCommunicationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.SecurityUtil;
import com.example.demo.service.AuditLogService;

@RestController
@RequestMapping({"/admin/compliance", "/api/admin/compliance"})
public class AdminComplianceController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private RegulatoryCommunicationRepository regulatoryCommunicationRepository;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/kyc-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<Map<String, Object>> getKycComplianceReport() {
        List<User> users = userRepository.findAll();

        long total = users.size();
        long approved = users.stream().filter(u -> u.getKycStatus() == KycStatus.APPROVED).count();
        long pending = users.stream().filter(u -> u.getKycStatus() == KycStatus.PENDING).count();
        long rejected = users.stream().filter(u -> u.getKycStatus() == KycStatus.REJECTED).count();
        long notSubmitted = users.stream().filter(u -> u.getKycStatus() == KycStatus.NOT_SUBMITTED).count();

        long lowRisk = users.stream().filter(u -> u.getRiskCategory() == RiskCategory.LOW).count();
        long medRisk = users.stream().filter(u -> u.getRiskCategory() == RiskCategory.MEDIUM).count();
        long highRisk = users.stream().filter(u -> u.getRiskCategory() == RiskCategory.HIGH).count();

        Map<String, Object> map = new HashMap<>();
        map.put("totalUsers", total);
        map.put("kycApproved", approved);
        map.put("kycPending", pending);
        map.put("kycRejected", rejected);
        map.put("kycNotSubmitted", notSubmitted);
        map.put("lowRiskUsers", lowRisk);
        map.put("mediumRiskUsers", medRisk);
        map.put("highRiskUsers", highRisk);
        map.put("cddScreenedPercent", total > 0 ? Math.round(((double) approved / total) * 100.0) : 0);

        return ResponseEntity.ok(map);
    }

    @GetMapping("/complaints")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPLIANCE_OFFICER', 'SUPPORT')")
    public ResponseEntity<List<Complaint>> getComplaints(@RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.valueOf(status.toUpperCase())));
        }
        return ResponseEntity.ok(complaintRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/complaints")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPLIANCE_OFFICER', 'SUPPORT')")
    public ResponseEntity<Complaint> createComplaint(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
        String userEmail = (String) body.get("userEmail");
        String subject = (String) body.get("subject");
        String desc = (String) body.get("description");
        String category = (String) body.getOrDefault("category", "TRADING");
        String priority = (String) body.getOrDefault("priority", "MEDIUM");
        int slaHours = body.get("slaHours") != null ? ((Number) body.get("slaHours")).intValue() : 48;

        Complaint c = new Complaint(userId, userEmail, subject, desc, category, priority, slaHours);
        Complaint saved = complaintRepository.save(c);

        auditLogService.logAction("CREATE_COMPLAINT", "COMPLAINT", saved.getId().toString(),
                null, subject, "Grievance ticket created", request);

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/complaints/{id}/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPLIANCE_OFFICER', 'SUPPORT')")
    public ResponseEntity<Complaint> assignComplaint(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String adminEmail = body.get("adminEmail");
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with ID: " + id));

        c.setAssignedToAdminEmail(adminEmail);
        c.setStatus(ComplaintStatus.ASSIGNED);
        Complaint saved = complaintRepository.save(c);

        auditLogService.logAction("ASSIGN_COMPLAINT", "COMPLAINT", id.toString(),
                "OPEN", "ASSIGNED", "Assigned to " + adminEmail, request);

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/complaints/{id}/resolve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPLIANCE_OFFICER', 'SUPPORT')")
    public ResponseEntity<Complaint> resolveComplaint(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String notes = body.get("resolutionNotes");
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with ID: " + id));

        c.setStatus(ComplaintStatus.RESOLVED);
        c.setResolutionNotes(notes);
        c.setResolvedAt(LocalDateTime.now());
        Complaint saved = complaintRepository.save(c);

        auditLogService.logAction("RESOLVE_COMPLAINT", "COMPLAINT", id.toString(),
                "ASSIGNED", "RESOLVED", notes, request);

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/regulatory-comms")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<List<RegulatoryCommunication>> getRegulatoryComms() {
        return ResponseEntity.ok(regulatoryCommunicationRepository.findAllByOrderByTimestampDesc());
    }

    @PostMapping("/regulatory-comms")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<RegulatoryCommunication> recordRegulatoryComm(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String regulator = body.get("regulator");
        String ref = body.get("referenceNumber");
        String subject = body.get("subject");
        String direction = body.getOrDefault("direction", "INBOUND");
        String status = body.getOrDefault("status", "PENDING");
        String details = body.get("details");

        User admin = securityUtil.getCurrentUser();
        String adminEmail = admin != null ? admin.getEmail() : "SYSTEM";

        RegulatoryCommunication comm = new RegulatoryCommunication(regulator, ref, subject, direction, status, details, adminEmail);
        RegulatoryCommunication saved = regulatoryCommunicationRepository.save(comm);

        auditLogService.logAction("RECORD_REGULATORY_COMM", "REGULATORY_COMM", saved.getId().toString(),
                null, regulator + ": " + subject, "Regulatory notice recorded", request);

        return ResponseEntity.ok(saved);
    }
}
