package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.CorporateAction;
import com.example.demo.model.Portfolio;
import com.example.demo.model.Stock;
import com.example.demo.model.User;
import com.example.demo.model.WalletTransaction;
import com.example.demo.repository.CorporateActionRepository;
import com.example.demo.repository.PortfolioRepository;
import com.example.demo.repository.StockRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WalletTransactionRepository;
import com.example.demo.security.SecurityUtil;

@Service
public class CorporateActionService {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private CorporateActionRepository corporateActionRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private SecurityUtil securityUtil;

    @Transactional
    public CorporateAction executeCorporateAction(Long stockId, String actionType, double ratioOrAmount,
                                                  String description, HttpServletRequest request) {
        if (ratioOrAmount <= 0) {
            throw new IllegalArgumentException("Ratio or amount must be greater than zero");
        }

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found with ID: " + stockId));

        User admin = securityUtil.getCurrentUser();
        String adminEmail = admin != null ? admin.getEmail() : "SYSTEM";

        List<Portfolio> holdings = portfolioRepository.findByStock(stock);
        int affectedCount = 0;
        double totalDisbursed = 0.0;

        String upperAction = actionType.toUpperCase().trim();

        if ("DIVIDEND".equals(upperAction)) {
            // Cash credited to user wallets
            for (Portfolio p : holdings) {
                if (p.getQuantity() > 0) {
                    double dividendAmount = Math.round(p.getQuantity() * ratioOrAmount * 100.0) / 100.0;
                    User user = p.getUser();
                    user.setBalance(Math.round((user.getBalance() + dividendAmount) * 100.0) / 100.0);
                    userRepository.save(user);

                    WalletTransaction wt = new WalletTransaction(
                            user, "TRADE_CREDIT", dividendAmount, user.getBalance(), "SUCCESS",
                            "DIV-" + stock.getSymbol() + "-" + System.currentTimeMillis(), "WALLET",
                            "Dividend of ₹" + ratioOrAmount + " per share for " + p.getQuantity() + " shares of " + stock.getSymbol()
                    );
                    walletTransactionRepository.save(wt);

                    totalDisbursed += dividendAmount;
                    affectedCount++;
                }
            }
        } else if ("SPLIT".equals(upperAction)) {
            // Share multiplier (e.g. 2 for 1:2 split)
            int multiplier = (int) ratioOrAmount;
            if (multiplier < 2) multiplier = 2;

            for (Portfolio p : holdings) {
                if (p.getQuantity() > 0) {
                    p.setQuantity(p.getQuantity() * multiplier);
                    p.setAverageBuyPrice(Math.round((p.getAverageBuyPrice() / multiplier) * 100.0) / 100.0);
                    portfolioRepository.save(p);
                    affectedCount++;
                }
            }
        } else if ("BONUS".equals(upperAction)) {
            // Bonus shares issued (e.g. 1 bonus for every 1 held -> ratio = 1)
            for (Portfolio p : holdings) {
                if (p.getQuantity() > 0) {
                    int bonusShares = (int) Math.floor(p.getQuantity() * ratioOrAmount);
                    if (bonusShares > 0) {
                        int newTotal = p.getQuantity() + bonusShares;
                        double newAvg = Math.round(((p.getQuantity() * p.getAverageBuyPrice()) / newTotal) * 100.0) / 100.0;
                        p.setQuantity(newTotal);
                        p.setAverageBuyPrice(newAvg);
                        portfolioRepository.save(p);
                        affectedCount++;
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported corporate action type: " + actionType);
        }

        CorporateAction ca = new CorporateAction(stock, upperAction, ratioOrAmount, description, adminEmail);
        ca.setAffectedHoldingsCount(affectedCount);
        ca.setTotalDisbursedOrAdjusted(Math.round(totalDisbursed * 100.0) / 100.0);
        CorporateAction saved = corporateActionRepository.save(ca);

        auditLogService.logAction("CORPORATE_ACTION_" + upperAction, "STOCK", stock.getSymbol(),
                null, upperAction + " ratio/amount: " + ratioOrAmount,
                description != null ? description : "Corporate action executed", request);

        return saved;
    }
}
