package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.model.ApprovalToken;
import com.example.demo.model.User;
import com.example.demo.model.UserLoginHistory;
import com.example.demo.model.enums.AccessStatus;
import com.example.demo.model.enums.AccountStatus;
import com.example.demo.model.enums.KycStatus;
import com.example.demo.repository.ApprovalTokenRepository;
import com.example.demo.repository.UserLoginHistoryRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtils;
import com.example.demo.security.LoginRateLimiter;
import com.example.demo.security.SecurityUtil;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApprovalTokenRepository approvalTokenRepository;

    @Autowired
    private UserLoginHistoryRepository userLoginHistoryRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginRateLimiter loginRateLimiter;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired(required = false)
    private HttpServletRequest httpServletRequest;

    @Transactional
    public String register(RegisterRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            return "Please provide all required fields";
        }

        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            return "Email already exists";
        }

        User user = new User();
        user.setUsername(request.getUsername() != null && !request.getUsername().isBlank()
                ? request.getUsername().trim()
                : request.getEmail().split("@")[0]);
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        user.setBalance(100000.0);
        user.setBlockedAmount(0.0);
        user.setRole("ROLE_USER");
        user.setAccessStatus(AccessStatus.PENDING_APPROVAL);
        user.setKycStatus(KycStatus.NOT_SUBMITTED);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Generate 48h single-use approval & decline tokens
        String approveTokenStr = UUID.randomUUID().toString();
        String declineTokenStr = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(48);

        ApprovalToken approveToken = new ApprovalToken(approveTokenStr, savedUser, "APPROVE", expiresAt);
        ApprovalToken declineToken = new ApprovalToken(declineTokenStr, savedUser, "DECLINE", expiresAt);
        approvalTokenRepository.save(approveToken);
        approvalTokenRepository.save(declineToken);

        // Dispatch admin notification email (with safe fallback logging)
        emailService.sendAdminApprovalRequestEmail(savedUser, approveTokenStr, declineTokenStr);

        return "Registration Successful. Your access request is awaiting admin approval.";
    }

    @Transactional
    public AuthResponse authenticate(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Email and password are required");
        }

        String email = request.getEmail().trim().toLowerCase();
        String ip = extractClientIp();
        String userAgent = httpServletRequest != null ? httpServletRequest.getHeader("User-Agent") : "Unknown";

        if (loginRateLimiter.isBlocked(email, ip)) {
            recordLoginAttempt(null, email, ip, userAgent, false, "Rate limit exceeded / Locked");
            throw new IllegalArgumentException("Too many failed attempts. Account is temporarily locked. Please try again in a few moments.");
        }

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            loginRateLimiter.recordFailedAttempt(email, ip);
            recordLoginAttempt(null, email, ip, userAgent, false, "User not found");
            throw new IllegalArgumentException("Invalid Email or Password");
        }

        User user = userOpt.get();

        // Check password: strictly require BCrypt hash match
        boolean matches = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!matches) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            loginRateLimiter.recordFailedAttempt(email, ip);

            if (user.getFailedLoginAttempts() >= 8) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(2));
                user.setLockReason("Temporary lock due to multiple failed login attempts");
            }
            userRepository.save(user);

            recordLoginAttempt(user.getId(), email, ip, userAgent, false, "Invalid password");
            throw new IllegalArgumentException("Invalid Email or Password");
        }

        // Validate Access Status
        if (user.getAccessStatus() == AccessStatus.PENDING_APPROVAL) {
            recordLoginAttempt(user.getId(), email, ip, userAgent, false, "Pending approval");
            throw new IllegalArgumentException("Your access request is awaiting admin approval");
        }

        if (user.getAccessStatus() == AccessStatus.DECLINED) {
            recordLoginAttempt(user.getId(), email, ip, userAgent, false, "Access declined");
            String reason = user.getDeclineReason() != null ? " Reason: " + user.getDeclineReason() : "";
            throw new IllegalArgumentException("Your account access request has been declined." + reason);
        }

        // Validate Account Status
        if (user.getAccountStatus() == AccountStatus.FROZEN) {
            recordLoginAttempt(user.getId(), email, ip, userAgent, false, "Account frozen");
            String reason = user.getFreezeReason() != null ? " Reason: " + user.getFreezeReason() : " Please contact support.";
            throw new IllegalArgumentException("Your account is frozen." + reason);
        }

        if (user.getLockedUntil() != null) {
            if (user.getLockedUntil().isAfter(LocalDateTime.now())) {
                recordLoginAttempt(user.getId(), email, ip, userAgent, false, "Account locked");
                throw new IllegalArgumentException("Your account is temporarily locked due to multiple failed login attempts. Please try again later.");
            } else {
                user.setLockedUntil(null);
                user.setFailedLoginAttempts(0);
                if (user.getAccountStatus() == AccountStatus.LOCKED) {
                    user.setAccountStatus(AccountStatus.ACTIVE);
                    user.setLockReason(null);
                }
            }
        }

        if (user.getAccountStatus() == AccountStatus.LOCKED) {
            recordLoginAttempt(user.getId(), email, ip, userAgent, false, "Account locked");
            throw new IllegalArgumentException("Your account is locked. Please contact support.");
        }

        if (user.getAccountStatus() == AccountStatus.DEACTIVATED) {
            recordLoginAttempt(user.getId(), email, ip, userAgent, false, "Account deactivated");
            throw new IllegalArgumentException("Your account is deactivated. Please contact support.");
        }

        // Login success: reset failure counter & rate limiter
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        loginRateLimiter.clear(email, ip);

        recordLoginAttempt(user.getId(), email, ip, userAgent, true, null);

        String token = jwtUtils.generateToken(user.getEmail());
        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getBalance(),
                "Login Successful"
        );
    }

    @Transactional
    public String processApprovalToken(String tokenStr, String reason) {
        User admin = securityUtil.getCurrentUser();
        if (admin == null || !admin.getRole().contains("ADMIN")) {
            throw new AccessDeniedException("Admin privileges required. Please login with an administrator account before processing approval links.");
        }

        ApprovalToken token = approvalTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        if (token.isUsed()) {
            throw new IllegalStateException("This action link has already been used");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("This action link has expired");
        }

        User user = token.getUser();
        boolean isApprove = "APPROVE".equalsIgnoreCase(token.getAction());

        if (isApprove) {
            user.setAccessStatus(AccessStatus.APPROVED);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            token.setUsed(true);
            token.setUsedAt(LocalDateTime.now());
            approvalTokenRepository.save(token);

            emailService.sendUserApprovalStatusEmail(user, true, null);
            auditLogService.logAction("APPROVE_USER_ACCESS", "USER", user.getId().toString(),
                    "PENDING_APPROVAL", "APPROVED", "Approved via secure link by " + admin.getEmail(), httpServletRequest);

            return "Applicant " + user.getUsername() + " (" + user.getEmail() + ") access has been APPROVED successfully.";
        } else {
            user.setAccessStatus(AccessStatus.DECLINED);
            user.setDeclineReason(reason != null && !reason.isBlank() ? reason : "Declined by admin");
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            token.setUsed(true);
            token.setUsedAt(LocalDateTime.now());
            approvalTokenRepository.save(token);

            emailService.sendUserApprovalStatusEmail(user, false, user.getDeclineReason());
            auditLogService.logAction("DECLINE_USER_ACCESS", "USER", user.getId().toString(),
                    "PENDING_APPROVAL", "DECLINED", user.getDeclineReason(), httpServletRequest);

            return "Applicant " + user.getUsername() + " (" + user.getEmail() + ") access has been DECLINED.";
        }
    }

    private void recordLoginAttempt(Long userId, String email, String ip, String userAgent, boolean success, String failureReason) {
        try {
            UserLoginHistory history = new UserLoginHistory(userId, email, ip, userAgent, detectDevice(userAgent), "Local", success, failureReason);
            userLoginHistoryRepository.save(history);
        } catch (Exception e) {
            // Non-blocking log save failure
        }
    }

    private String detectDevice(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) return "Mobile";
        if (ua.contains("tablet") || ua.contains("ipad")) return "Tablet";
        return "Desktop";
    }

    private String extractClientIp() {
        if (httpServletRequest == null) return "127.0.0.1";
        String xf = httpServletRequest.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return httpServletRequest.getRemoteAddr() != null ? httpServletRequest.getRemoteAddr() : "127.0.0.1";
    }
}