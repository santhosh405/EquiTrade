package com.example.demo.Controller.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.ApiResponse;
import com.example.demo.model.RiskAlert;
import com.example.demo.model.enums.RiskAlertSeverity;
import com.example.demo.model.enums.RiskAlertStatus;
import com.example.demo.repository.PortfolioRepository;
import com.example.demo.repository.RiskAlertRepository;
import com.example.demo.repository.StockRepository;
import com.example.demo.service.RiskService;

@RestController
@RequestMapping({"/admin/risk", "/api/admin/risk"})
public class AdminRiskController {

    @Autowired
    private RiskAlertRepository riskAlertRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private RiskService riskService;

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER')")
    public ResponseEntity<List<RiskAlert>> getAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity) {

        List<RiskAlert> list = riskAlertRepository.findAllByOrderByCreatedAtDesc();
        List<RiskAlert> filtered = list.stream().filter(a -> {
            if (status != null && !status.isBlank() && (a.getStatus() == null || !a.getStatus().name().equalsIgnoreCase(status))) {
                return false;
            }
            if (severity != null && !severity.isBlank() && (a.getSeverity() == null || !a.getSeverity().name().equalsIgnoreCase(severity))) {
                return false;
            }
            return true;
        }).toList();

        return ResponseEntity.ok(filtered);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER')")
    public ResponseEntity<Map<String, Object>> getRiskDashboard() {
        long openAlerts = riskAlertRepository.countByStatus(RiskAlertStatus.OPEN);
        long acknowledgedAlerts = riskAlertRepository.countByStatus(RiskAlertStatus.ACKNOWLEDGED);
        long resolvedAlerts = riskAlertRepository.countByStatus(RiskAlertStatus.RESOLVED);
        long restrictedStocksCount = stockRepository.findAll().stream().filter(s -> s.isTradingRestricted()).count();
        long openPositionsCount = portfolioRepository.count();

        Map<String, Object> map = new HashMap<>();
        map.put("openAlerts", openAlerts);
        map.put("acknowledgedAlerts", acknowledgedAlerts);
        map.put("resolvedAlerts", resolvedAlerts);
        map.put("restrictedStocksCount", restrictedStocksCount);
        map.put("openPositionsCount", openPositionsCount);
        map.put("intradayLeverageFactor", "5x Standard / 1x Overnight");

        return ResponseEntity.ok(map);
    }

    @PostMapping("/alerts/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER')")
    public ResponseEntity<RiskAlert> acknowledgeAlert(@PathVariable Long id, HttpServletRequest request) {
        RiskAlert alert = riskService.acknowledgeAlert(id, request);
        return ResponseEntity.ok(alert);
    }

    @PostMapping("/alerts/{id}/resolve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER')")
    public ResponseEntity<RiskAlert> resolveAlert(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String actionTaken = body.get("actionTaken");
        RiskAlert alert = riskService.resolveAlert(id, actionTaken, request);
        return ResponseEntity.ok(alert);
    }

    @PostMapping("/trigger-square-off-check")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER')")
    public ResponseEntity<ApiResponse> triggerSquareOffCheck() {
        riskService.runAutoSquareOffMonitoring();
        return ResponseEntity.ok(new ApiResponse(true, "Auto square-off and margin scan initiated. Any breach has been logged to Risk Alerts."));
    }
}
