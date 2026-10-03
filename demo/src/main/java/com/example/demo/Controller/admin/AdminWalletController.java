package com.example.demo.Controller.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.ApiResponse;
import com.example.demo.model.User;
import com.example.demo.model.WalletTransaction;
import com.example.demo.model.WithdrawalRequest;
import com.example.demo.model.enums.WithdrawalStatus;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WalletTransactionRepository;
import com.example.demo.repository.WithdrawalRequestRepository;
import com.example.demo.service.WalletService;

@RestController
@RequestMapping({"/admin/wallet", "/api/admin/wallet"})
public class AdminWalletController {

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private WithdrawalRequestRepository withdrawalRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletService walletService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE')")
    public ResponseEntity<Map<String, Object>> getWalletSummary() {
        List<User> users = userRepository.findAll();
        double totalBalance = users.stream().mapToDouble(User::getBalance).sum();
        double totalBlocked = users.stream().mapToDouble(User::getBlockedAmount).sum();
        long pendingWithdrawals = withdrawalRequestRepository.countByStatus(WithdrawalStatus.PENDING_REVIEW) +
                                  withdrawalRequestRepository.countByStatus(WithdrawalStatus.UNDER_REVIEW);

        Map<String, Object> map = new HashMap<>();
        map.put("totalPlatformBalance", Math.round(totalBalance * 100.0) / 100.0);
        map.put("totalBlockedFunds", Math.round(totalBlocked * 100.0) / 100.0);
        map.put("pendingWithdrawalsCount", pendingWithdrawals);
        map.put("totalTransactionsCount", walletTransactionRepository.count());

        return ResponseEntity.ok(map);
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE')")
    public ResponseEntity<Map<String, Object>> getTransactions(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<WalletTransaction> list = walletTransactionRepository.findAllByOrderByCreatedAtDesc();

        List<WalletTransaction> filtered = list.stream().filter(t -> {
            if (type != null && !type.isBlank() && !t.getType().equalsIgnoreCase(type.trim())) {
                return false;
            }
            if (status != null && !status.isBlank() && !t.getStatus().equalsIgnoreCase(status.trim())) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());

        int start = Math.min(page * size, filtered.size());
        int end = Math.min(start + size, filtered.size());
        List<WalletTransaction> paginated = filtered.subList(start, end);

        Map<String, Object> resp = new HashMap<>();
        resp.put("content", paginated);
        resp.put("totalElements", filtered.size());
        resp.put("totalPages", (int) Math.ceil((double) filtered.size() / size));
        resp.put("page", page);
        resp.put("size", size);

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/withdrawals")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE')")
    public ResponseEntity<List<WithdrawalRequest>> getWithdrawals(
            @RequestParam(required = false) String status) {

        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(withdrawalRequestRepository.findByStatusOrderByCreatedAtDesc(WithdrawalStatus.valueOf(status.toUpperCase())));
        }
        return ResponseEntity.ok(withdrawalRequestRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/withdrawals/{id}/review")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE')")
    public ResponseEntity<WithdrawalRequest> reviewWithdrawal(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String notes = body.get("notes");
        WithdrawalRequest req = walletService.reviewWithdrawal(id, notes, request);
        return ResponseEntity.ok(req);
    }

    @PostMapping("/withdrawals/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE')")
    public ResponseEntity<WithdrawalRequest> approveWithdrawal(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String reason = body.get("reason");
        WithdrawalRequest req = walletService.approveWithdrawal(id, reason, request);
        return ResponseEntity.ok(req);
    }

    @PostMapping("/withdrawals/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE')")
    public ResponseEntity<WithdrawalRequest> rejectWithdrawal(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String reason = body.get("reason");
        WithdrawalRequest req = walletService.rejectWithdrawal(id, reason, request);
        return ResponseEntity.ok(req);
    }

    @PostMapping("/refund")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE')")
    public ResponseEntity<ApiResponse> issueRefund(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        Long userId = ((Number) body.get("userId")).longValue();
        double amount = ((Number) body.get("amount")).doubleValue();
        String reason = (String) body.get("reason");

        walletService.issueRefund(userId, amount, reason, request);
        return ResponseEntity.ok(new ApiResponse(true, "Refund of ₹" + amount + " successfully credited to user #" + userId));
    }
}
