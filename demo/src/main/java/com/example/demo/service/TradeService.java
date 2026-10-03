package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.TradeRequest;
import com.example.demo.dto.TransactionDTO;
import com.example.demo.model.Portfolio;
import com.example.demo.model.RiskAlert;
import com.example.demo.model.Stock;
import com.example.demo.model.Transaction;
import com.example.demo.model.User;
import com.example.demo.model.WalletTransaction;
import com.example.demo.model.enums.AccountStatus;
import com.example.demo.model.enums.OrderStatus;
import com.example.demo.model.enums.OrderType;
import com.example.demo.model.enums.RiskAlertSeverity;
import com.example.demo.repository.PortfolioRepository;
import com.example.demo.repository.RiskAlertRepository;
import com.example.demo.repository.StockRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WalletTransactionRepository;
import com.example.demo.security.SecurityUtil;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class TradeService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private RiskAlertRepository riskAlertRepository;

    @Autowired
    private FeeCalculationService feeCalculationService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private SecurityUtil securityUtil;

    @Transactional
    public String buyStock(TradeRequest request) {
        if (request == null) {
            return "Invalid trade request";
        }
        Long stockId = request.getStockId();
        if (stockId == null && request.getId() != null) {
            stockId = request.getId();
        }
        if (stockId == null || stockId <= 0 || request.getQuantity() <= 0) {
            return "Invalid trade request: valid stockId and quantity > 0 required";
        }

        // SECURITY: always resolve from JWT — never trust client-supplied userId.
        User user = securityUtil.getCurrentUser();
        if (user == null) {
            return "User not found";
        }

        // Check account freeze / restrictions
        if (user.getAccountStatus() == AccountStatus.FROZEN) {
            return "Account is FROZEN. Trading operations are restricted.";
        }
        if (user.getAccountStatus() == AccountStatus.DEACTIVATED || user.getAccountStatus() == AccountStatus.LOCKED) {
            return "Account is inactive or locked. Trading is unavailable.";
        }

        Optional<Stock> stockOpt = stockRepository.findById(stockId);
        if (stockOpt.isEmpty()) {
            return "Stock not found";
        }

        Stock stock = stockOpt.get();
        if (stock.isTradingRestricted()) {
            return "Trading for " + stock.getSymbol() + " is currently suspended: " +
                   (stock.getRestrictionReason() != null ? stock.getRestrictionReason() : "Regulatory halt");
        }

        double grossPrice = stock.getPrice() * request.getQuantity();
        FeeCalculationService.FeeBreakdown fees = feeCalculationService.calculateFees(grossPrice, "BUY");
        double totalRequired = grossPrice + fees.getTotalFees();

        if (user.getBalance() < totalRequired) {
            return "Insufficient Balance. Total required (including ₹" + fees.getTotalFees() + " statutory fees): ₹" + Math.round(totalRequired * 100.0) / 100.0;
        }

        // Deduct balance
        user.setBalance(Math.round((user.getBalance() - totalRequired) * 100.0) / 100.0);
        userRepository.save(user);

        // Update portfolio
        Optional<Portfolio> existingOpt = portfolioRepository.findByUserAndStock(user, stock);
        Portfolio portfolio;
        if (existingOpt.isPresent()) {
            portfolio = existingOpt.get();
            int oldQty = portfolio.getQuantity();
            double oldAvg = portfolio.getAverageBuyPrice() > 0 ? portfolio.getAverageBuyPrice() : stock.getPrice();
            int newQty = oldQty + request.getQuantity();
            double newAvg = ((oldQty * oldAvg) + grossPrice) / newQty;
            portfolio.setQuantity(newQty);
            portfolio.setAverageBuyPrice(Math.round(newAvg * 100.0) / 100.0);
        } else {
            portfolio = new Portfolio(null, user, stock, request.getQuantity(), stock.getPrice());
        }
        portfolioRepository.save(portfolio);

        // Record Executed Order
        LocalDateTime now = LocalDateTime.now();
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setStock(stock);
        transaction.setQuantity(request.getQuantity());
        transaction.setPrice(stock.getPrice());
        transaction.setType("BUY");
        transaction.setDate(now);
        transaction.setStatus(OrderStatus.EXECUTED);
        transaction.setOrderType(OrderType.MARKET);
        transaction.setBrokerageFee(fees.getBrokerage());
        transaction.setTaxes(fees.getGst() + fees.getStt() + fees.getTransactionCharges() + fees.getPlatformCharges());
        transaction.setTotalAmount(Math.round(totalRequired * 100.0) / 100.0);
        transaction.setStatusTimeline("Order placed at " + now + " -> Executed at market price ₹" + stock.getPrice() + " with fees ₹" + fees.getTotalFees());
        transaction.setUpdatedAt(now);
        transactionRepository.save(transaction);

        // Record Ledger Debit
        WalletTransaction ledger = new WalletTransaction(
                user, "TRADE_DEBIT", totalRequired, user.getBalance(), "SUCCESS",
                "ORD-" + transaction.getId(), "WALLET", "Purchase of " + request.getQuantity() + " shares of " + stock.getSymbol()
        );
        walletTransactionRepository.save(ledger);

        // Risk monitoring: abnormal order size check
        if (request.getQuantity() > 5000 || grossPrice > 1000000) {
            RiskAlert alert = new RiskAlert(
                    user.getId(), user.getEmail(), "ABNORMAL_ORDER_SIZE", RiskAlertSeverity.HIGH,
                    "Unusually large BUY order of " + request.getQuantity() + " " + stock.getSymbol() +
                    " valued at ₹" + Math.round(grossPrice * 100.0) / 100.0
            );
            riskAlertRepository.save(alert);
        }

        return "Stock Purchased Successfully";
    }

    @Transactional
    public String sellStock(TradeRequest request) {
        if (request == null) {
            return "Invalid trade request";
        }
        if (request.getQuantity() <= 0) {
            return "Quantity must be greater than zero";
        }

        Long targetStockId = request.getStockId();
        if (targetStockId == null && request.getId() != null) {
            targetStockId = request.getId();
        }
        if (targetStockId == null && request.getHoldingId() != null) {
            Optional<Portfolio> pOpt = portfolioRepository.findById(request.getHoldingId());
            if (pOpt.isPresent() && pOpt.get().getStock() != null) {
                targetStockId = pOpt.get().getStock().getId();
            }
        }

        if (targetStockId == null || targetStockId <= 0) {
            return "Valid stock ID or holding ID is required";
        }

        // SECURITY: always resolve from JWT — never trust client-supplied userId.
        User user = securityUtil.getCurrentUser();
        if (user == null) {
            return "User not found";
        }

        if (user.getAccountStatus() == AccountStatus.FROZEN) {
            return "Account is FROZEN. Trading operations are restricted.";
        }
        if (user.getAccountStatus() == AccountStatus.DEACTIVATED || user.getAccountStatus() == AccountStatus.LOCKED) {
            return "Account is inactive or locked. Trading is unavailable.";
        }

        Optional<Stock> stockOpt = stockRepository.findById(targetStockId);
        Stock stock;
        if (stockOpt.isEmpty()) {
            Optional<Portfolio> holdingById = portfolioRepository.findById(targetStockId);
            if (holdingById.isPresent() && holdingById.get().getStock() != null) {
                stock = holdingById.get().getStock();
            } else {
                return "Stock not found";
            }
        } else {
            stock = stockOpt.get();
        }

        if (stock.isTradingRestricted()) {
            return "Trading for " + stock.getSymbol() + " is currently suspended: " +
                   (stock.getRestrictionReason() != null ? stock.getRestrictionReason() : "Regulatory halt");
        }

        Optional<Portfolio> holdingOpt = portfolioRepository.findByUserAndStock(user, stock);
        if (holdingOpt.isEmpty() || holdingOpt.get().getQuantity() <= 0) {
            return "You do not own any shares of " + stock.getSymbol() + " to sell";
        }

        Portfolio portfolio = holdingOpt.get();
        if (portfolio.getQuantity() < request.getQuantity()) {
            return "Insufficient shares to sell. You own " + portfolio.getQuantity() + " shares, but tried to sell " + request.getQuantity();
        }

        int remaining = portfolio.getQuantity() - request.getQuantity();

        if (remaining == 0) {
            portfolioRepository.delete(portfolio);
        } else {
            portfolio.setQuantity(remaining);
            portfolioRepository.save(portfolio);
        }

        double grossProceeds = stock.getPrice() * request.getQuantity();
        FeeCalculationService.FeeBreakdown fees = feeCalculationService.calculateFees(grossProceeds, "SELL");
        double netProceeds = Math.max(0, grossProceeds - fees.getTotalFees());

        user.setBalance(Math.round((user.getBalance() + netProceeds) * 100.0) / 100.0);
        userRepository.save(user);

        LocalDateTime now = LocalDateTime.now();
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setStock(stock);
        transaction.setQuantity(request.getQuantity());
        transaction.setPrice(stock.getPrice());
        transaction.setType("SELL");
        transaction.setDate(now);
        transaction.setStatus(OrderStatus.EXECUTED);
        transaction.setOrderType(OrderType.MARKET);
        transaction.setBrokerageFee(fees.getBrokerage());
        transaction.setTaxes(fees.getGst() + fees.getStt() + fees.getTransactionCharges() + fees.getPlatformCharges());
        transaction.setTotalAmount(Math.round(netProceeds * 100.0) / 100.0);
        transaction.setStatusTimeline("Order placed at " + now + " -> Executed at market price ₹" + stock.getPrice() + " with fees ₹" + fees.getTotalFees());
        transaction.setUpdatedAt(now);
        transactionRepository.save(transaction);

        // Record Ledger Credit
        WalletTransaction ledger = new WalletTransaction(
                user, "TRADE_CREDIT", netProceeds, user.getBalance(), "SUCCESS",
                "ORD-" + transaction.getId(), "WALLET", "Sale of " + request.getQuantity() + " shares of " + stock.getSymbol()
        );
        walletTransactionRepository.save(ledger);

        // Risk monitoring
        if (request.getQuantity() > 5000 || grossProceeds > 1000000) {
            RiskAlert alert = new RiskAlert(
                    user.getId(), user.getEmail(), "ABNORMAL_ORDER_SIZE", RiskAlertSeverity.HIGH,
                    "Unusually large SELL order of " + request.getQuantity() + " " + stock.getSymbol() +
                    " valued at ₹" + Math.round(grossProceeds * 100.0) / 100.0
            );
            riskAlertRepository.save(alert);
        }

        return "Stock Sold Successfully";
    }

    public List<TransactionDTO> getHistory(User user) {
        List<Transaction> list;
        if (user != null) {
            list = transactionRepository.findByUserOrderByDateDesc(user);
        } else {
            list = transactionRepository.findAllByOrderByDateDesc();
        }

        List<TransactionDTO> dtoList = new ArrayList<>();
        for (Transaction t : list) {
            Stock s = t.getStock();
            String sym = s != null ? s.getSymbol() : "UNKNOWN";
            String comp = s != null ? s.getCompanyName() : "Unknown Company";
            Long sId = s != null ? s.getId() : null;
            dtoList.add(new TransactionDTO(
                t.getId(),
                t.getType(),
                sId,
                sym,
                comp,
                t.getQuantity(),
                t.getPrice(),
                t.getDate()
            ));
        }
        return dtoList;
    }

    @Transactional
    public void cancelPendingOrder(Long orderId, String reason, HttpServletRequest request) {
        Transaction order = transactionRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        if (order.getStatus() == OrderStatus.EXECUTED) {
            throw new IllegalStateException("Executed trades are READ-ONLY and strictly immutable by compliance regulations. Cannot modify or cancel.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled.");
        }

        OrderStatus before = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledReason(reason != null && !reason.isBlank() ? reason : "Cancelled by admin");
        order.setStatusTimeline(order.getStatusTimeline() + " -> Cancelled at " + LocalDateTime.now() + " Reason: " + order.getCancelledReason());
        order.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(order);

        auditLogService.logAction("CANCEL_ORDER", "ORDER", orderId.toString(),
                before.name(), OrderStatus.CANCELLED.name(), reason, request);
    }

    @Transactional
    public void addInvestigationNote(Long orderId, String note, HttpServletRequest request) {
        Transaction order = transactionRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        String before = order.getInvestigationNotes();
        String updated = (before != null && !before.isBlank())
                ? before + "\n[" + LocalDateTime.now() + "] " + note
                : "[" + LocalDateTime.now() + "] " + note;
        order.setInvestigationNotes(updated);
        order.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(order);

        auditLogService.logAction("ADD_INVESTIGATION_NOTE", "ORDER", orderId.toString(),
                before, updated, "Investigation note updated", request);
    }

    public void enforceTradeImmutability(Long orderId) {
        Transaction order = transactionRepository.findById(orderId).orElse(null);
        if (order != null && order.getStatus() == OrderStatus.EXECUTED) {
            throw new IllegalStateException("Compliance Rule: Executed trade #" + orderId + " is strictly READ-ONLY. No edits or deletions are permitted.");
        }
    }
}