package com.example.demo.Controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.AdminDashboardDTO;
import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.MostTradedStockDTO;
import com.example.demo.dto.StockRequest;
import com.example.demo.model.Stock;
import com.example.demo.model.enums.AccessStatus;
import com.example.demo.model.enums.KycStatus;
import com.example.demo.model.enums.RiskAlertStatus;
import com.example.demo.model.enums.WithdrawalStatus;
import com.example.demo.repository.*;
import com.example.demo.service.StockService;

@RestController
@RequestMapping({"/admin", "/api/admin"})
public class AdminController {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockService stockService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RiskAlertRepository riskAlertRepository;

    @Autowired
    private WithdrawalRequestRepository withdrawalRequestRepository;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'KYC_OFFICER', 'SUPPORT', 'RISK_OFFICER', 'FINANCE', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<AdminDashboardDTO> getDashboard() {
        long totalUsers = userRepository.count();
        long totalTrades = transactionRepository.count();
        long totalStocks = stockRepository.count();
        double totalVolume = transactionRepository.findAll().stream()
                .mapToDouble(t -> t.getQuantity() * t.getPrice())
                .sum();

        List<Object[]> rawMostTraded = transactionRepository.findMostTradedStocks();
        List<MostTradedStockDTO> mostTraded = new ArrayList<>();
        for (int i = 0; i < Math.min(5, rawMostTraded.size()); i++) {
            Object[] row = rawMostTraded.get(i);
            Long sId = ((Number) row[0]).longValue();
            String sym = (String) row[1];
            String comp = (String) row[2];
            long cnt = ((Number) row[3]).longValue();
            mostTraded.add(new MostTradedStockDTO(sId, sym, comp, cnt));
        }

        if (mostTraded.isEmpty()) {
            List<Stock> allStocks = stockRepository.findAll();
            for (int i = 0; i < Math.min(4, allStocks.size()); i++) {
                Stock s = allStocks.get(i);
                mostTraded.add(new MostTradedStockDTO(s.getId(), s.getSymbol(), s.getCompanyName(), 0));
            }
        }

        // Additional telemetry for Phase 4 dashboard
        long pendingAccessRequests = userRepository.countByAccessStatus(AccessStatus.PENDING_APPROVAL);
        long pendingKycCount = userRepository.countByKycStatus(KycStatus.PENDING);
        long openRiskAlertsCount = riskAlertRepository.countByStatus(RiskAlertStatus.OPEN);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long ordersTodayCount = transactionRepository.countByDateAfter(startOfDay);
        long pendingWithdrawalsCount = withdrawalRequestRepository.countByStatus(WithdrawalStatus.PENDING_REVIEW) +
                                      withdrawalRequestRepository.countByStatus(WithdrawalStatus.UNDER_REVIEW);

        AdminDashboardDTO dto = new AdminDashboardDTO(
                totalUsers,
                totalTrades,
                totalStocks,
                Math.round(totalVolume * 100.0) / 100.0,
                mostTraded,
                pendingAccessRequests,
                pendingKycCount,
                openRiskAlertsCount,
                ordersTodayCount,
                pendingWithdrawalsCount
        );

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/stocks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER')")
    public List<Stock> getStocks() {
        return stockRepository.findAll();
    }

    @PostMapping("/stocks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER')")
    public ResponseEntity<Stock> addStock(@RequestBody StockRequest request) {
        Stock stock = new Stock();
        stock.setSymbol(request.getSymbol() != null ? request.getSymbol().toUpperCase().trim() : "NEW");
        stock.setCompanyName(request.getCompanyName() != null ? request.getCompanyName().trim() : "New Listing");
        stock.setPrice(request.getPrice() > 0 ? request.getPrice() : 100.0);
        stock.setChangePercent(request.getChangePercent());
        stock.setSector(request.getSector() != null && !request.getSector().isBlank() ? request.getSector().trim() : "General");

        Stock created = stockService.addStock(stock);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/stocks/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER')")
    public ResponseEntity<?> updateStock(@PathVariable Long id, @RequestBody StockRequest request) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid stock ID"));
        }
        Stock updated = stockService.updateStock(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping({"/stock/{id}", "/stocks/{id}"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse> deleteStock(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid stock ID"));
        }
        boolean deleted = stockService.deleteStock(id);
        if (deleted) {
            return ResponseEntity.ok(new ApiResponse(true, "Stock Deleted Successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, "Stock Not Found"));
        }
    }
}