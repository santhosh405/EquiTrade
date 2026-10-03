package com.example.demo.Controller.admin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.SecurityIncident;
import com.example.demo.model.User;
import com.example.demo.model.UserLoginHistory;
import com.example.demo.model.enums.SecurityIncidentStatus;
import com.example.demo.repository.SecurityIncidentRepository;
import com.example.demo.repository.UserLoginHistoryRepository;
import com.example.demo.security.SecurityUtil;
import com.example.demo.service.AuditLogService;

@RestController
@RequestMapping({"/admin/fraud", "/api/admin/fraud"})
public class AdminFraudController {

    @Autowired
    private UserLoginHistoryRepository userLoginHistoryRepository;

    @Autowired
    private SecurityIncidentRepository securityIncidentRepository;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/logins")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<List<UserLoginHistory>> getLogins(@RequestParam(required = false) Boolean suspicious) {
        if (Boolean.TRUE.equals(suspicious)) {
            return ResponseEntity.ok(userLoginHistoryRepository.findBySuspiciousTrueOrderByTimestampDesc());
        }
        return ResponseEntity.ok(userLoginHistoryRepository.findAll());
    }

    @GetMapping("/incidents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<List<SecurityIncident>> getIncidents(@RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(securityIncidentRepository.findByStatusOrderByCreatedAtDesc(SecurityIncidentStatus.valueOf(status.toUpperCase())));
        }
        return ResponseEntity.ok(securityIncidentRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/incidents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<SecurityIncident> createIncident(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String type = body.get("incidentType");
        String targetType = body.get("targetType");
        String targetId = body.get("targetId");
        String severity = body.get("severity");
        String details = body.get("details");

        SecurityIncident incident = new SecurityIncident(type, targetType, targetId, severity, details);
        User admin = securityUtil.getCurrentUser();
        if (admin != null) {
            incident.setAssignedToAdminEmail(admin.getEmail());
        }
        SecurityIncident saved = securityIncidentRepository.save(incident);

        auditLogService.logAction("CREATE_SECURITY_INCIDENT", "SECURITY_INCIDENT", saved.getId().toString(),
                null, type, "Incident recorded for investigation", request);

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/incidents/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<SecurityIncident> updateIncidentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        SecurityIncident incident = securityIncidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found with ID: " + id));

        String statusStr = body.get("status");
        String notes = body.get("investigationNotes");
        String resolution = body.get("resolution");

        SecurityIncidentStatus targetStatus = SecurityIncidentStatus.valueOf(statusStr.toUpperCase());
        incident.setStatus(targetStatus);
        if (notes != null) incident.setInvestigationNotes(notes);
        if (resolution != null) incident.setResolution(resolution);
        if (targetStatus == SecurityIncidentStatus.CLOSED) {
            incident.setClosedAt(LocalDateTime.now());
        }
        SecurityIncident saved = securityIncidentRepository.save(incident);

        auditLogService.logAction("UPDATE_INCIDENT_STATUS", "SECURITY_INCIDENT", id.toString(),
                incident.getStatus().name(), targetStatus.name(), resolution != null ? resolution : notes, request);

        return ResponseEntity.ok(saved);
    }
}
