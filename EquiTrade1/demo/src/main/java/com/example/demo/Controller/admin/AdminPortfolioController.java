package com.example.demo.Controller.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.CorporateAction;
import com.example.demo.model.Portfolio;
import com.example.demo.repository.CorporateActionRepository;
import com.example.demo.repository.PortfolioRepository;
import com.example.demo.service.CorporateActionService;

@RestController
@RequestMapping({"/admin/portfolios", "/api/admin/portfolios"})
public class AdminPortfolioController {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private CorporateActionRepository corporateActionRepository;

    @Autowired
    private CorporateActionService corporateActionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE', 'SUPPORT')")
    public ResponseEntity<List<Map<String, Object>>> getAllPortfolios() {
        List<Portfolio> list = portfolioRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Portfolio p : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("userId", p.getUser() != null ? p.getUser().getId() : null);
            map.put("userEmail", p.getUser() != null ? p.getUser().getEmail() : "");
            map.put("userName", p.getUser() != null ? p.getUser().getUsername() : "");
            map.put("stockId", p.getStock() != null ? p.getStock().getId() : null);
            map.put("symbol", p.getStock() != null ? p.getStock().getSymbol() : "");
            map.put("companyName", p.getStock() != null ? p.getStock().getCompanyName() : "");
            map.put("quantity", p.getQuantity());
            map.put("averageBuyPrice", p.getAverageBuyPrice());

            double curPrice = p.getStock() != null ? p.getStock().getPrice() : 0.0;
            double curVal = p.getQuantity() * curPrice;
            double costVal = p.getQuantity() * p.getAverageBuyPrice();
            double pnl = curVal - costVal;
            double pnlPercent = costVal > 0 ? (pnl / costVal) * 100.0 : 0.0;

            map.put("currentPrice", curPrice);
            map.put("currentValue", Math.round(curVal * 100.0) / 100.0);
            map.put("pnl", Math.round(pnl * 100.0) / 100.0);
            map.put("pnlPercent", Math.round(pnlPercent * 100.0) / 100.0);
            map.put("dematStatus", "CDSL_ACTIVE");

            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE', 'SUPPORT')")
    public ResponseEntity<List<Portfolio>> getUserPortfolios(@PathVariable Long userId) {
        return ResponseEntity.ok(portfolioRepository.findByUserId(userId));
    }

    @PostMapping("/corporate-action")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE')")
    public ResponseEntity<CorporateAction> executeCorporateAction(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        Long stockId = ((Number) body.get("stockId")).longValue();
        String actionType = (String) body.get("actionType");
        double ratioOrAmount = ((Number) body.get("ratioOrAmount")).doubleValue();
        String description = (String) body.get("description");

        CorporateAction ca = corporateActionService.executeCorporateAction(stockId, actionType, ratioOrAmount, description, request);
        return ResponseEntity.ok(ca);
    }

    @GetMapping("/corporate-actions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE')")
    public ResponseEntity<List<CorporateAction>> getCorporateActions() {
        return ResponseEntity.ok(corporateActionRepository.findAllByOrderByExecutionDateDesc());
    }
}
