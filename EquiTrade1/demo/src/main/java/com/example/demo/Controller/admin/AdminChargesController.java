package com.example.demo.Controller.admin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.FeeRule;
import com.example.demo.model.User;
import com.example.demo.repository.FeeRuleRepository;
import com.example.demo.security.SecurityUtil;
import com.example.demo.service.AuditLogService;
import com.example.demo.service.FeeCalculationService;

@RestController
@RequestMapping({"/admin/charges", "/api/admin/charges"})
public class AdminChargesController {

    @Autowired
    private FeeRuleRepository feeRuleRepository;

    @Autowired
    private FeeCalculationService feeCalculationService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private SecurityUtil securityUtil;

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE')")
    public ResponseEntity<FeeRule> getActiveFeeRule() {
        return ResponseEntity.ok(feeCalculationService.getActiveFeeRule());
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE')")
    public ResponseEntity<List<FeeRule>> getFeeRuleHistory() {
        return ResponseEntity.ok(feeRuleRepository.findAllByOrderByVersionDesc());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE')")
    public ResponseEntity<FeeRule> createNewFeeRuleVersion(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String reason = (String) body.get("reasonForChange");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A mandatory reason must be provided when introducing new tariff/fee rules");
        }

        User admin = securityUtil.getCurrentUser();
        String adminEmail = admin != null ? admin.getEmail() : "SYSTEM";

        List<FeeRule> existingRules = feeRuleRepository.findAllByOrderByVersionDesc();
        int nextVersion = existingRules.isEmpty() ? 1 : existingRules.get(0).getVersion() + 1;

        // Deactivate previous active rule(s) - versioned, never overwritten
        for (FeeRule old : existingRules) {
            if (old.isActive()) {
                old.setActive(false);
                old.setEndDate(LocalDateTime.now());
                feeRuleRepository.save(old);
            }
        }

        FeeRule newRule = new FeeRule();
        newRule.setVersion(nextVersion);
        newRule.setName((String) body.getOrDefault("name", "Tariff Schedule v" + nextVersion));
        newRule.setBrokerageType(((String) body.getOrDefault("brokerageType", "FLAT")).toUpperCase());
        newRule.setBrokerageValue(((Number) body.getOrDefault("brokerageValue", 20.0)).doubleValue());
        newRule.setTransactionChargePercent(((Number) body.getOrDefault("transactionChargePercent", 0.00345)).doubleValue());
        newRule.setPlatformCharge(((Number) body.getOrDefault("platformCharge", 0.0)).doubleValue());
        newRule.setGstPercent(((Number) body.getOrDefault("gstPercent", 18.0)).doubleValue());
        newRule.setSttPercent(((Number) body.getOrDefault("sttPercent", 0.1)).doubleValue());
        newRule.setActive(true);
        newRule.setStartDate(LocalDateTime.now());
        newRule.setReasonForChange(reason);
        newRule.setCreatedByAdminEmail(adminEmail);
        newRule.setCreatedAt(LocalDateTime.now());

        FeeRule saved = feeRuleRepository.save(newRule);

        auditLogService.logAction("CREATE_FEE_RULE", "FEE_RULE", "v" + nextVersion,
                "v" + (nextVersion - 1), "v" + nextVersion, reason, request);

        return ResponseEntity.ok(saved);
    }
}
