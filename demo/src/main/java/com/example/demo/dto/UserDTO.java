package com.example.demo.dto;

import com.example.demo.model.User;
import com.example.demo.model.enums.AccessStatus;
import com.example.demo.model.enums.AccountStatus;
import com.example.demo.model.enums.KycStatus;

import java.time.LocalDateTime;

/**
 * Safe projection of the User entity for API responses.
 * Intentionally omits: password, failedLoginAttempts, lockedUntil,
 * lockReason, freezeReason, panNumber, maskedAadhaar, riskCategory,
 * forcePasswordReset, declineReason (only returned to the user themselves
 * when appropriate).
 */
public class UserDTO {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private double balance;
    private String role;
    private AccessStatus accessStatus;
    private KycStatus kycStatus;
    private AccountStatus accountStatus;
    private String declineReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserDTO() {}

    /** Full mapping used by admin endpoints. */
    public static UserDTO fromUser(User user) {
        if (user == null) return null;
        UserDTO dto = new UserDTO();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.email = user.getEmail();
        dto.phone = user.getPhone();
        dto.balance = user.getBalance();
        dto.role = user.getRole();
        dto.accessStatus = user.getAccessStatus();
        dto.kycStatus = user.getKycStatus();
        dto.accountStatus = user.getAccountStatus();
        dto.declineReason = user.getDeclineReason();
        dto.createdAt = user.getCreatedAt();
        dto.updatedAt = user.getUpdatedAt();
        return dto;
    }

    /** Restricted mapping for /auth/me — no balance or admin fields. */
    public static UserDTO fromUserSelf(User user) {
        if (user == null) return null;
        UserDTO dto = new UserDTO();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.email = user.getEmail();
        dto.phone = user.getPhone();
        dto.balance = user.getBalance();
        dto.role = user.getRole();
        dto.accessStatus = user.getAccessStatus();
        dto.kycStatus = user.getKycStatus();
        dto.accountStatus = user.getAccountStatus();
        dto.createdAt = user.getCreatedAt();
        dto.updatedAt = user.getUpdatedAt();
        return dto;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public double getBalance() { return balance; }
    public String getRole() { return role; }
    public AccessStatus getAccessStatus() { return accessStatus; }
    public KycStatus getKycStatus() { return kycStatus; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public String getDeclineReason() { return declineReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ── Setters (kept for Jackson) ────────────────────────────────────────────

    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setBalance(double balance) { this.balance = balance; }
    public void setRole(String role) { this.role = role; }
    public void setAccessStatus(AccessStatus accessStatus) { this.accessStatus = accessStatus; }
    public void setKycStatus(KycStatus kycStatus) { this.kycStatus = kycStatus; }
    public void setAccountStatus(AccountStatus accountStatus) { this.accountStatus = accountStatus; }
    public void setDeclineReason(String declineReason) { this.declineReason = declineReason; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
