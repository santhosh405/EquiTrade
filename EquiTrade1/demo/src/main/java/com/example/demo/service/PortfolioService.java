package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.PerformancePointDTO;
import com.example.demo.dto.PortfolioHoldingDTO;
import com.example.demo.dto.PortfolioPerformanceDTO;
import com.example.demo.dto.PortfolioSummaryDTO;
import com.example.demo.dto.StockPriceHistoryDTO;
import com.example.demo.model.Portfolio;
import com.example.demo.model.Stock;
import com.example.demo.model.User;
import com.example.demo.repository.PortfolioRepository;
import com.example.demo.repository.StockRepository;

@Service
public class PortfolioService {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockService stockService;

    public List<Portfolio> getPortfolio(User user) {
        if (user == null) return List.of();
        return portfolioRepository.findByUser(user);
    }

    public PortfolioSummaryDTO getPortfolioSummary(User user) {
        if (user == null) {
            return new PortfolioSummaryDTO(0.0, 0.0, 0.0, 0.0, 0.0, List.of());
        }

        List<Portfolio> holdings = portfolioRepository.findByUser(user);
        List<PortfolioHoldingDTO> dtoList = new ArrayList<>();

        double totalInvested = 0.0;
        double totalCurrentValue = 0.0;

        for (Portfolio p : holdings) {
            Stock s = p.getStock();
            if (s == null) continue;

            int qty = p.getQuantity();
            if (qty <= 0) continue;

            double avgCost = p.getAverageBuyPrice() > 0 ? p.getAverageBuyPrice() : s.getPrice();
            double curPrice = s.getPrice();
            double invested = qty * avgCost;
            double curValue = qty * curPrice;
            double pnl = curValue - invested;
            double pnlPercent = invested > 0 ? (pnl / invested) * 100.0 : 0.0;

            totalInvested += invested;
            totalCurrentValue += curValue;

            dtoList.add(new PortfolioHoldingDTO(
                p.getId(),
                s.getId(),
                s.getSymbol(),
                s.getCompanyName(),
                qty,
                Math.round(avgCost * 100.0) / 100.0,
                curPrice,
                Math.round(curValue * 100.0) / 100.0,
                Math.round(pnl * 100.0) / 100.0,
                Math.round(pnlPercent * 100.0) / 100.0,
                s.getSector() != null ? s.getSector() : "General"
            ));
        }

        double totalProfitLoss = totalCurrentValue - totalInvested;
        double totalProfitLossPercent = totalInvested > 0 ? (totalProfitLoss / totalInvested) * 100.0 : 0.0;

        return new PortfolioSummaryDTO(
            Math.round(user.getBalance() * 100.0) / 100.0,
            Math.round(totalInvested * 100.0) / 100.0,
            Math.round(totalCurrentValue * 100.0) / 100.0,
            Math.round(totalProfitLoss * 100.0) / 100.0,
            Math.round(totalProfitLossPercent * 100.0) / 100.0,
            dtoList
        );
    }

    public PortfolioPerformanceDTO getPortfolioPerformance(User user, String range) {
        String cleanRange = (range != null && !range.isBlank()) ? range.toUpperCase().trim() : "1M";
        PortfolioSummaryDTO summary = getPortfolioSummary(user);

        double cashBalance = (user != null) ? user.getBalance() : 100000.0;
        double totalInvested = summary.getTotalInvested();
        double currentStockValue = summary.getTotalCurrentValue();
        double currentTotalPortfolioValue = currentStockValue + cashBalance;
        double totalProfitLoss = summary.getTotalProfitLoss();
        double totalProfitLossPercent = summary.getTotalProfitLossPercent();

        List<Portfolio> holdings = (user != null) ? portfolioRepository.findByUser(user) : List.of();

        // Fetch NIFTYBEES history as benchmark
        List<StockPriceHistoryDTO> benchmarkHistory = stockService.getPriceHistory("NIFTYBEES", cleanRange);

        // Fetch history for each holding
        Map<String, List<StockPriceHistoryDTO>> holdingHistories = new HashMap<>();
        for (Portfolio p : holdings) {
            if (p.getStock() != null && p.getQuantity() > 0) {
                String sym = p.getStock().getSymbol();
                if (!holdingHistories.containsKey(sym)) {
                    holdingHistories.put(sym, stockService.getPriceHistory(sym, cleanRange));
                }
            }
        }

        List<PerformancePointDTO> points = new ArrayList<>();
        double benchmarkFirstClose = (!benchmarkHistory.isEmpty() && benchmarkHistory.get(0).getClose() > 0)
                ? benchmarkHistory.get(0).getClose() : 260.0;
        double benchmarkLastClose = (!benchmarkHistory.isEmpty())
                ? benchmarkHistory.get(benchmarkHistory.size() - 1).getClose() : benchmarkFirstClose;
        double benchmarkTotalReturn = ((benchmarkLastClose - benchmarkFirstClose) / benchmarkFirstClose) * 100.0;

        // If benchmarkHistory is available, use its timestamps as our time anchor
        if (!benchmarkHistory.isEmpty()) {
            int pointCount = benchmarkHistory.size();
            for (int i = 0; i < pointCount; i++) {
                StockPriceHistoryDTO benchPoint = benchmarkHistory.get(i);
                double benchClose = benchPoint.getClose();
                double benchReturn = ((benchClose - benchmarkFirstClose) / benchmarkFirstClose) * 100.0;

                double portValue = cashBalance;
                double investedSoFar = 0.0;

                if (!holdings.isEmpty()) {
                    for (Portfolio p : holdings) {
                        if (p.getStock() == null || p.getQuantity() <= 0) continue;
                        String sym = p.getStock().getSymbol();
                        List<StockPriceHistoryDTO> hList = holdingHistories.get(sym);
                        double stockClose = p.getStock().getPrice();
                        if (hList != null && !hList.isEmpty()) {
                            int idx = Math.min(i, hList.size() - 1);
                            stockClose = hList.get(idx).getClose();
                        }
                        portValue += p.getQuantity() * stockClose;
                        investedSoFar += p.getQuantity() * p.getAverageBuyPrice();
                    }
                } else {
                    // For demo/empty portfolios, show cash baseline
                    portValue = cashBalance;
                    investedSoFar = 0.0;
                }

                double initialPortValue = cashBalance + totalInvested;
                double portReturn = (initialPortValue > 0)
                        ? ((portValue - initialPortValue) / initialPortValue) * 100.0 : 0.0;

                long ts = 0L;
                try {
                    ts = Long.parseLong(benchPoint.getTime());
                } catch (Exception e) {
                    if (benchPoint.getRecordedAt() != null) {
                        ts = benchPoint.getRecordedAt().atZone(ZoneId.systemDefault()).toEpochSecond();
                    }
                }

                points.add(new PerformancePointDTO(
                    benchPoint.getTime(),
                    ts,
                    Math.round(portValue * 100.0) / 100.0,
                    Math.round(investedSoFar * 100.0) / 100.0,
                    Math.round(benchClose * 100.0) / 100.0,
                    Math.round(portReturn * 100.0) / 100.0,
                    Math.round(benchReturn * 100.0) / 100.0
                ));
            }
        }

        // Guarantee final point reflects exact current portfolio value
        if (!points.isEmpty()) {
            PerformancePointDTO lastPoint = points.get(points.size() - 1);
            lastPoint.setPortfolioValue(Math.round(currentTotalPortfolioValue * 100.0) / 100.0);
            lastPoint.setInvestedValue(Math.round(totalInvested * 100.0) / 100.0);
            lastPoint.setPortfolioReturnPercent(Math.round(totalProfitLossPercent * 100.0) / 100.0);
        }

        return new PortfolioPerformanceDTO(
            cleanRange,
            Math.round(currentTotalPortfolioValue * 100.0) / 100.0,
            Math.round(totalInvested * 100.0) / 100.0,
            Math.round(totalProfitLoss * 100.0) / 100.0,
            Math.round(totalProfitLossPercent * 100.0) / 100.0,
            "NIFTY 50 (NIFTYBEES)",
            Math.round(benchmarkTotalReturn * 100.0) / 100.0,
            points
        );
    }
}