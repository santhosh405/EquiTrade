package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.User;
import com.example.demo.model.WalletTransaction;
import com.example.demo.model.WithdrawalRequest;
import com.example.demo.model.enums.WithdrawalStatus;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WalletTransactionRepository;
import com.example.demo.repository.WithdrawalRequestRepository;
import com.example.demo.security.SecurityUtil;

@Service
public class WalletService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private WithdrawalRequestRepository withdrawalRequestRepository;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private AuditLogService auditLogService;

    @Transactional
    public WithdrawalRequest requestWithdrawal(Long userId, double amount, String bankAccount, String ifsc, String bankName) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (user.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient available balance for withdrawal");
        }

        // Block funds from available balance
        user.setBalance(Math.round((user.getBalance() - amount) * 100.0) / 100.0);
        user.setBlockedAmount(Math.round((user.getBlockedAmount() + amount) * 100.0) / 100.0);
        userRepository.save(user);

        WithdrawalRequest req = new WithdrawalRequest(user, amount, bankAccount, ifsc, bankName);
        WithdrawalRequest saved = withdrawalRequestRepository.save(req);

        WalletTransaction txn = new WalletTransaction(
                user, "WITHDRAWAL", amount, user.getBalance(), "PENDING",
                "WDR-" + saved.getId(), "NEFT", "Withdrawal request placed - funds blocked"
        );
        walletTransactionRepository.save(txn);

        return saved;
    }

    // Step 1: Maker review
    @Transactional
    public WithdrawalRequest reviewWithdrawal(Long withdrawalId, String notes, HttpServletRequest request) {
        WithdrawalRequest req = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal request not found"));

        if (req.getStatus() != WithdrawalStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only pending withdrawal requests can be reviewed");
        }

        User admin = securityUtil.getCurrentUser();
        if (admin == null) {
            throw new IllegalStateException("Admin context not found");
        }

        req.setStatus(WithdrawalStatus.UNDER_REVIEW);
        req.setReviewedByAdminId(admin.getId());
        req.setReviewedByEmail(admin.getEmail());
        req.setReviewedAt(LocalDateTime.now());
        req.setReviewNotes(notes);

        WithdrawalRequest saved = withdrawalRequestRepository.save(req);

        auditLogService.logAction("REVIEW_WITHDRAWAL", "WITHDRAWAL", withdrawalId.toString(),
                WithdrawalStatus.PENDING_REVIEW.name(), WithdrawalStatus.UNDER_REVIEW.name(),
                notes != null ? notes : "Review completed", request);

        return saved;
    }

    // Step 2: Checker approval (MUST BE A DIFFERENT ADMIN!)
    @Transactional
    public WithdrawalRequest approveWithdrawal(Long withdrawalId, String reason, HttpServletRequest request) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A mandatory approval reason must be provided for financial transactions");
        }

        WithdrawalRequest req = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal request not found"));

        if (req.getStatus() == WithdrawalStatus.APPROVED) {
            throw new IllegalStateException("Withdrawal request has already been approved");
        }

        User admin = securityUtil.getCurrentUser();
        if (admin == null) {
            throw new IllegalStateException("Admin context not found");
        }

        // MAKER-CHECKER ENFORCEMENT: Approver cannot be the same admin who reviewed!
        if (req.getReviewedByAdminId() != null && req.getReviewedByAdminId().equals(admin.getId())) {
            throw new IllegalStateException("Maker-Checker violation: The approving admin cannot be the same admin who reviewed this withdrawal request.");
        }

        req.setStatus(WithdrawalStatus.APPROVED);
        req.setApprovedByAdminId(admin.getId());
        req.setApprovedByEmail(admin.getEmail());
        req.setApprovedAt(LocalDateTime.now());
        req.setApprovalNotes(reason);

        // Deduct from blocked amount
        User user = req.getUser();
        user.setBlockedAmount(Math.max(0, Math.round((user.getBlockedAmount() - req.getAmount()) * 100.0) / 100.0));
        userRepository.save(user);

        WithdrawalRequest saved = withdrawalRequestRepository.save(req);

        WalletTransaction txn = new WalletTransaction(
                user, "WITHDRAWAL", req.getAmount(), user.getBalance(), "SUCCESS",
                "WDR-" + saved.getId(), "NEFT", "Disbursed via bank transfer. Approval reason: " + reason
        );
        walletTransactionRepository.save(txn);

        auditLogService.logAction("APPROVE_WITHDRAWAL", "WITHDRAWAL", withdrawalId.toString(),
                WithdrawalStatus.UNDER_REVIEW.name(), WithdrawalStatus.APPROVED.name(),
                reason, request);

        return saved;
    }

    // Checker rejection (Refunds blocked amount back to user balance)
    @Transactional
    public WithdrawalRequest rejectWithdrawal(Long withdrawalId, String reason, HttpServletRequest request) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A mandatory rejection reason must be provided");
        }

        WithdrawalRequest req = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal request not found"));

        if (req.getStatus() == WithdrawalStatus.APPROVED) {
            throw new IllegalStateException("Approved withdrawals cannot be rejected");
        }

        User admin = securityUtil.getCurrentUser();

        // MAKER-CHECKER ENFORCEMENT: Rejecter cannot be the same admin who reviewed
        if (req.getReviewedByAdminId() != null && admin != null && req.getReviewedByAdminId().equals(admin.getId())) {
            throw new IllegalStateException("Maker-Checker violation: The rejecting admin cannot be the same admin who reviewed this withdrawal request.");
        }

        req.setStatus(WithdrawalStatus.REJECTED);
        req.setRejectionReason(reason);

        // Unblock funds and restore user balance
        User user = req.getUser();
        user.setBlockedAmount(Math.max(0, Math.round((user.getBlockedAmount() - req.getAmount()) * 100.0) / 100.0));
        user.setBalance(Math.round((user.getBalance() + req.getAmount()) * 100.0) / 100.0);
        userRepository.save(user);

        WithdrawalRequest saved = withdrawalRequestRepository.save(req);

        WalletTransaction txn = new WalletTransaction(
                user, "REFUND", req.getAmount(), user.getBalance(), "SUCCESS",
                "REF-" + saved.getId(), "WALLET", "Withdrawal rejected and refunded. Reason: " + reason
        );
        walletTransactionRepository.save(txn);

        auditLogService.logAction("REJECT_WITHDRAWAL", "WITHDRAWAL", withdrawalId.toString(),
                req.getStatus().name(), WithdrawalStatus.REJECTED.name(),
                reason, request);

        return saved;
    }

    @Transactional
    public WalletTransaction issueRefund(Long userId, double amount, String reason, HttpServletRequest request) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than zero");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A mandatory reason must be provided for issuing administrative refunds");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        user.setBalance(Math.round((user.getBalance() + amount) * 100.0) / 100.0);
        userRepository.save(user);

        WalletTransaction refundTxn = new WalletTransaction(
                user, "REFUND", amount, user.getBalance(), "SUCCESS",
                "REF-" + System.currentTimeMillis(), "WALLET", reason
        );
        WalletTransaction saved = walletTransactionRepository.save(refundTxn);

        auditLogService.logAction("ISSUE_REFUND", "USER", userId.toString(),
                null, "₹" + amount, reason, request);

        return saved;
    }
}
