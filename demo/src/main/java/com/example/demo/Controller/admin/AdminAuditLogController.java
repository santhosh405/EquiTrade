package com.example.demo.Controller.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.AuditLog;
import com.example.demo.repository.AuditLogRepository;

@RestController
@RequestMapping({"/admin/audit-logs", "/api/admin/audit-logs"})
public class AdminAuditLogController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(required = false) String adminEmail,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {

        List<AuditLog> all = auditLogRepository.findAllByOrderByTimestampDesc();

        List<AuditLog> filtered = all.stream().filter(log -> {
            if (adminEmail != null && !adminEmail.isBlank() &&
                    (log.getAdminEmail() == null || !log.getAdminEmail().toLowerCase().contains(adminEmail.toLowerCase().trim()))) {
                return false;
            }
            if (targetType != null && !targetType.isBlank() &&
                    (log.getTargetType() == null || !log.getTargetType().equalsIgnoreCase(targetType.trim()))) {
                return false;
            }
            if (action != null && !action.isBlank() &&
                    (log.getAction() == null || !log.getAction().toLowerCase().contains(action.toLowerCase().trim()))) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());

        int start = Math.min(page * size, filtered.size());
        int end = Math.min(start + size, filtered.size());
        List<AuditLog> paginated = filtered.subList(start, end);

        Map<String, Object> resp = new HashMap<>();
        resp.put("content", paginated);
        resp.put("totalElements", filtered.size());
        resp.put("totalPages", (int) Math.ceil((double) filtered.size() / size));
        resp.put("page", page);
        resp.put("size", size);

        return ResponseEntity.ok(resp);
    }
}
