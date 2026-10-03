package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Portfolio;
import com.example.demo.model.RiskAlert;
import com.example.demo.model.Transaction;
import com.example.demo.model.User;
import com.example.demo.model.enums.AccountStatus;
import com.example.demo.model.enums.OrderStatus;
import com.example.demo.model.enums.RiskAlertSeverity;
import com.example.demo.model.enums.RiskAlertStatus;
import com.example.demo.repository.PortfolioRepository;
import com.example.demo.repository.RiskAlertRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.SecurityUtil;

@Service
public class RiskService {

    private static final Logger logger = LoggerFactory.getLogger(RiskService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private RiskAlertRepository riskAlertRepository;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private AuditLogService auditLogService;

    @Transactional
    public User freezeAccount(Long userId, String reason, HttpServletRequest request) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A mandatory reason must be provided to freeze an account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        AccountStatus prev = user.getAccountStatus();
        user.setAccountStatus(AccountStatus.FROZEN);
        user.setFreezeReason(reason);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        // Cancel all pending orders for this user
        List<Transaction> pendingOrders = transactionRepository.findByUserOrderByDateDesc(user);
        int cancelledCount = 0;
        for (Transaction t : pendingOrders) {
            if (t.getStatus() == OrderStatus.PENDING) {
                t.setStatus(OrderStatus.CANCELLED);
                t.setCancelledReason("Account frozen by risk administration: " + reason);
                t.setStatusTimeline(t.getStatusTimeline() + " -> Cancelled due to account freeze at " + LocalDateTime.now());
                transactionRepository.save(t);
                cancelledCount++;
            }
        }

        // Flag open positions in RiskAlert
        List<Portfolio> holdings = portfolioRepository.findByUser(user);
        String desc = "Account frozen. Reason: " + reason + ". " + cancelledCount + " pending orders cancelled. " + holdings.size() + " open positions flagged for monitoring.";
        RiskAlert alert = new RiskAlert(user.getId(), user.getEmail(), "ACCOUNT_FROZEN", RiskAlertSeverity.HIGH, desc);
        riskAlertRepository.save(alert);

        auditLogService.logAction("FREEZE_ACCOUNT", "USER", userId.toString(),
                prev.name(), AccountStatus.FROZEN.name(), reason, request);

        return saved;
    }

    @Transactional
    public User unfreezeAccount(Long userId, String reason, HttpServletRequest request) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A mandatory reason must be provided to unfreeze an account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        AccountStatus prev = user.getAccountStatus();
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setFreezeReason(null);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        auditLogService.logAction("UNFREEZE_ACCOUNT", "USER", userId.toString(),
                prev.name(), AccountStatus.ACTIVE.name(), reason, request);

        return saved;
    }

    @Transactional
    public RiskAlert acknowledgeAlert(Long alertId, HttpServletRequest request) {
        RiskAlert alert = riskAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Risk alert not found with ID: " + alertId));

        alert.setStatus(RiskAlertStatus.ACKNOWLEDGED);
        RiskAlert saved = riskAlertRepository.save(alert);

        auditLogService.logAction("ACKNOWLEDGE_RISK_ALERT", "RISK_ALERT", alertId.toString(),
                RiskAlertStatus.OPEN.name(), RiskAlertStatus.ACKNOWLEDGED.name(), "Acknowledged by officer", request);

        return saved;
    }

    @Transactional
    public RiskAlert resolveAlert(Long alertId, String actionTaken, HttpServletRequest request) {
        RiskAlert alert = riskAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Risk alert not found with ID: " + alertId));

        User admin = securityUtil.getCurrentUser();
        String adminEmail = admin != null ? admin.getEmail() : "SYSTEM";

        alert.setStatus(RiskAlertStatus.RESOLVED);
        alert.setActionTaken(actionTaken != null && !actionTaken.isBlank() ? actionTaken : "Reviewed and resolved");
        alert.setResolvedByAdminEmail(adminEmail);
        alert.setResolvedAt(LocalDateTime.now());
        RiskAlert saved = riskAlertRepository.save(alert);

        auditLogService.logAction("RESOLVE_RISK_ALERT", "RISK_ALERT", alertId.toString(),
                RiskAlertStatus.ACKNOWLEDGED.name(), RiskAlertStatus.RESOLVED.name(), alert.getActionTaken(), request);

        return saved;
    }

    // Auto square-off monitoring (@Scheduled simulation every 5 minutes)
    @Scheduled(fixedDelay = 300000)
    public void runAutoSquareOffMonitoring() {
        try {
            List<User> users = userRepository.findAll();
            for (User u : users) {
                if (u.getAccountStatus() == AccountStatus.ACTIVE) {
                    List<Portfolio> holdings = portfolioRepository.findByUser(u);
                    double totalLoss = 0.0;
                    for (Portfolio p : holdings) {
                        double curPrice = p.getStock() != null ? p.getStock().getPrice() : 0.0;
                        double cost = p.getQuantity() * p.getAverageBuyPrice();
                        double curVal = p.getQuantity() * curPrice;
                        if (curVal < cost) {
                            totalLoss += (cost - curVal);
                        }
                    }
                    // If unrealized loss exceeds 70% of available cash balance, raise margin warning alert
                    if (totalLoss > 0 && totalLoss > (u.getBalance() * 0.7)) {
                        boolean exists = riskAlertRepository.findByStatusOrderByCreatedAtDesc(RiskAlertStatus.OPEN).stream()
                                .anyMatch(a -> u.getId().equals(a.getUserId()) && "MARGIN_SHORTFALL".equals(a.getAlertType()));
                        if (!exists) {
                            RiskAlert alert = new RiskAlert(
                                    u.getId(), u.getEmail(), "MARGIN_SHORTFALL", RiskAlertSeverity.MEDIUM,
                                    "User position loss (₹" + Math.round(totalLoss) + ") exceeds 70% of available balance (₹" + Math.round(u.getBalance()) + "). Margin warning active."
                            );
                            riskAlertRepository.save(alert);
                            logger.info("[RISK ENGINE] Margin shortfall alert created for {}", u.getEmail());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("Auto square-off monitoring scan skipped: {}", ex.getMessage());
        }
    }
}
