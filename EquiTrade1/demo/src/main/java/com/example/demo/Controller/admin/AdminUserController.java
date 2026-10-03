package com.example.demo.Controller.admin;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.UserDTO;
import com.example.demo.model.User;
import com.example.demo.model.UserLoginHistory;
import com.example.demo.model.enums.AccessStatus;
import com.example.demo.model.enums.AccountStatus;
import com.example.demo.repository.UserLoginHistoryRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuditLogService;
import com.example.demo.service.EmailService;
import com.example.demo.service.RiskService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping({"/admin", "/api/admin"})
public class AdminUserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLoginHistoryRepository userLoginHistoryRepository;

    @Autowired
    private RiskService riskService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuditLogService auditLogService;

    // ── User listing & detail ─────────────────────────────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPPORT', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String accessStatus,
            @RequestParam(required = false) String kycStatus,
            @RequestParam(required = false) String accountStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<User> all = userRepository.findAll();

        List<UserDTO> filtered = all.stream().filter(u -> {
            if (query != null && !query.isBlank()) {
                String q = query.toLowerCase().trim();
                boolean matchesName  = u.getUsername() != null && u.getUsername().toLowerCase().contains(q);
                boolean matchesEmail = u.getEmail()    != null && u.getEmail().toLowerCase().contains(q);
                if (!matchesName && !matchesEmail) return false;
            }
            if (role != null && !role.isBlank() && !u.getRole().equalsIgnoreCase(role)) return false;
            if (accessStatus != null && !accessStatus.isBlank()
                    && (u.getAccessStatus() == null || !u.getAccessStatus().name().equalsIgnoreCase(accessStatus))) return false;
            if (kycStatus != null && !kycStatus.isBlank()
                    && (u.getKycStatus() == null || !u.getKycStatus().name().equalsIgnoreCase(kycStatus))) return false;
            if (accountStatus != null && !accountStatus.isBlank()
                    && (u.getAccountStatus() == null || !u.getAccountStatus().name().equalsIgnoreCase(accountStatus))) return false;
            return true;
        }).map(UserDTO::fromUser).collect(Collectors.toList());

        int start     = Math.min(page * size, filtered.size());
        int end       = Math.min(start + size, filtered.size());
        List<UserDTO> paginated = filtered.subList(start, end);

        Map<String, Object> response = new HashMap<>();
        response.put("content",       paginated);
        response.put("totalElements", filtered.size());
        response.put("totalPages",    (int) Math.ceil((double) filtered.size() / size));
        response.put("page",          page);
        response.put("size",          size);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPPORT', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
        return ResponseEntity.ok(UserDTO.fromUser(user));
    }

    // ── Status mutations ──────────────────────────────────────────────────────

    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPPORT')")
    public ResponseEntity<ApiResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String statusStr = body.get("status");
        String reason    = body.get("reason");
        if (statusStr == null || statusStr.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Status is required"));
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        AccountStatus prev   = user.getAccountStatus();
        AccountStatus target = AccountStatus.valueOf(statusStr.toUpperCase());
        user.setAccountStatus(target);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        auditLogService.logAction("UPDATE_ACCOUNT_STATUS", "USER", id.toString(),
                prev.name(), target.name(), reason != null ? reason : "Status modified", request);

        return ResponseEntity.ok(new ApiResponse(true, "Account status updated to " + target.name()));
    }

    @PostMapping("/users/{id}/freeze")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER')")
    public ResponseEntity<ApiResponse> freezeUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        riskService.freezeAccount(id, body.get("reason"), request);
        return ResponseEntity.ok(new ApiResponse(true, "User account #" + id + " has been FROZEN. Open orders cancelled."));
    }

    @PostMapping("/users/{id}/unfreeze")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER')")
    public ResponseEntity<ApiResponse> unfreezeUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        riskService.unfreezeAccount(id, body.get("reason"), request);
        return ResponseEntity.ok(new ApiResponse(true, "User account #" + id + " has been UNFROZEN and restored to active state."));
    }

    @PostMapping("/users/{id}/lock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPPORT')")
    public ResponseEntity<ApiResponse> toggleLockUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        boolean lock   = Boolean.parseBoolean(body.getOrDefault("lock", "true"));
        String  reason = body.get("reason");

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        AccountStatus prev = user.getAccountStatus();
        if (lock) {
            user.setAccountStatus(AccountStatus.LOCKED);
            user.setLockReason(reason != null ? reason : "Administrative lock");
            user.setLockedUntil(LocalDateTime.now().plusHours(24));
        } else {
            user.setAccountStatus(AccountStatus.ACTIVE);
            user.setLockReason(null);
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }
        userRepository.save(user);

        auditLogService.logAction(lock ? "LOCK_USER" : "UNLOCK_USER", "USER", id.toString(),
                prev.name(), user.getAccountStatus().name(), reason, request);

        return ResponseEntity.ok(new ApiResponse(true, lock ? "User account locked." : "User account unlocked."));
    }

    @PostMapping("/users/{id}/reset-security")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPPORT')")
    public ResponseEntity<ApiResponse> resetSecuritySettings(
            @PathVariable Long id,
            HttpServletRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        user.setForcePasswordReset(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        if (user.getAccountStatus() == AccountStatus.LOCKED) {
            user.setAccountStatus(AccountStatus.ACTIVE);
        }
        userRepository.save(user);

        auditLogService.logAction("RESET_SECURITY_SETTINGS", "USER", id.toString(),
                null, "forcePasswordReset=true", "Force password reset initiated", request);

        return ResponseEntity.ok(new ApiResponse(true, "Security settings reset. User will be prompted to reset password on next login."));
    }

    // ── Login history ─────────────────────────────────────────────────────────

    @GetMapping("/users/{id}/login-history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPPORT', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<List<UserLoginHistory>> getUserLoginHistory(@PathVariable Long id) {
        return ResponseEntity.ok(userLoginHistoryRepository.findByUserIdOrderByTimestampDesc(id));
    }

    // ── Access-request workflow ───────────────────────────────────────────────

    @GetMapping("/access-requests/pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'KYC_OFFICER', 'SUPPORT', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<List<UserDTO>> getPendingAccessRequests() {
        List<UserDTO> list = userRepository.findByAccessStatus(AccessStatus.PENDING_APPROVAL)
                .stream().map(UserDTO::fromUser).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/access-requests/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'KYC_OFFICER', 'SUPPORT', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse> approveAccessRequest(@PathVariable Long id, HttpServletRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        user.setAccessStatus(AccessStatus.APPROVED);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        emailService.sendUserApprovalStatusEmail(user, true, null);
        auditLogService.logAction("APPROVE_USER_ACCESS", "USER", id.toString(),
                "PENDING_APPROVAL", "APPROVED", "Approved via admin console", request);

        return ResponseEntity.ok(new ApiResponse(true, "User " + user.getUsername() + " access APPROVED."));
    }

    @PostMapping("/access-requests/{id}/decline")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'KYC_OFFICER', 'SUPPORT', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse> declineAccessRequest(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        String reason = body != null ? body.get("reason") : "Eligibility requirements not met";
        user.setAccessStatus(AccessStatus.DECLINED);
        user.setDeclineReason(reason);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        emailService.sendUserApprovalStatusEmail(user, false, reason);
        auditLogService.logAction("DECLINE_USER_ACCESS", "USER", id.toString(),
                "PENDING_APPROVAL", "DECLINED", reason, request);

        return ResponseEntity.ok(new ApiResponse(true, "User " + user.getUsername() + " access DECLINED."));
    }
}
