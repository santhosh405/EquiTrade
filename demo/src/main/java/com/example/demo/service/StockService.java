package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.PagedStockResponse;
import com.example.demo.dto.StockPriceHistoryDTO;
import com.example.demo.dto.StockRequest;
import com.example.demo.model.Stock;
import com.example.demo.model.StockPriceHistory;
import com.example.demo.model.enums.InstrumentType;
import com.example.demo.repository.PortfolioRepository;
import com.example.demo.repository.StockPriceHistoryRepository;
import com.example.demo.repository.StockRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.WatchlistRepository;

@Service
public class StockService {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceHistoryRepository stockPriceHistoryRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public List<Stock> getAllStocks() {
        List<Stock> list = stockRepository.findAll();
        populateSparklines(list);
        return list;
    }

    public Stock addStock(Stock stock) {
        if (stock.getSector() == null || stock.getSector().isBlank()) {
            stock.setSector("General");
        }
        Stock saved = stockRepository.save(stock);
        // Seed initial history
        ensureStockHistorySeeded(saved);
        populateSparkline(saved);
        return saved;
    }

    public Optional<Stock> getStockById(Long id) {
        Optional<Stock> stock = stockRepository.findById(id);
        stock.ifPresent(this::populateSparkline);
        return stock;
    }

    public Optional<Stock> getStockBySymbol(String symbol) {
        if (symbol == null) return Optional.empty();
        Optional<Stock> stock = stockRepository.findBySymbolIgnoreCase(symbol.trim());
        stock.ifPresent(this::populateSparkline);
        return stock;
    }

    public List<Stock> searchStocks(String query) {
        List<Stock> list;
        if (query == null || query.isBlank()) {
            list = stockRepository.findAll();
        } else {
            String q = query.trim();
            list = stockRepository.findBySymbolContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(q, q);
        }
        populateSparklines(list);
        return list;
    }

    public List<String> getDistinctSectors() {
        return stockRepository.findDistinctSectors();
    }

    public List<Stock> getTopGainers() {
        List<Stock> list = stockRepository.findTop4ByOrderByChangePercentDesc();
        populateSparklines(list);
        return list;
    }

    public List<Stock> getTopLosers() {
        List<Stock> list = stockRepository.findTop4ByOrderByChangePercentAsc();
        populateSparklines(list);
        return list;
    }

    public PagedStockResponse getPagedStocks(String query, String sector, Double minPrice, Double maxPrice, int page, int size, String sortBy, String sortDir) {
        String sortProperty = "price";
        if ("changePercent".equalsIgnoreCase(sortBy)) {
            sortProperty = "changePercent";
        } else if ("symbol".equalsIgnoreCase(sortBy)) {
            sortProperty = "symbol";
        } else if ("companyName".equalsIgnoreCase(sortBy)) {
            sortProperty = "companyName";
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, sortProperty));

        String q = (query != null && !query.isBlank()) ? query.trim() : null;
        String s = (sector != null && !sector.isBlank()) ? sector.trim() : null;

        Page<Stock> pagedResult = stockRepository.filterStocks(q, s, minPrice, maxPrice, pageable);
        List<Stock> content = pagedResult.getContent();
        populateSparklines(content);

        return new PagedStockResponse(
            content,
            pagedResult.getNumber(),
            pagedResult.getSize(),
            pagedResult.getTotalElements(),
            pagedResult.getTotalPages(),
            pagedResult.isLast()
        );
    }

    public void populateSparkline(Stock stock) {
        if (stock == null) return;
        List<StockPriceHistory> history = stockPriceHistoryRepository.findTop40ByStockOrderByRecordedAtDesc(stock);
        List<Double> spark = new ArrayList<>();
        if (history != null && history.size() >= 30) {
            for (int i = 29; i >= 0; i--) {
                spark.add(history.get(i).getPrice());
            }
        } else {
            double cur = stock.getPrice();
            double change = stock.getChangePercent();
            for (int i = 29; i >= 0; i--) {
                double pct = (i / 29.0) * (change / 100.0);
                double noise = Math.sin(i * 0.7) * 0.005;
                double p = Math.round((cur / (1.0 + pct) + (cur * noise)) * 100.0) / 100.0;
                spark.add(Math.max(0.5, p));
            }
            spark.set(spark.size() - 1, cur);
        }
        stock.setSparkline(spark);
    }

    public void populateSparklines(List<Stock> stocks) {
        if (stocks == null) return;
        for (Stock s : stocks) {
            populateSparkline(s);
        }
    }

    public void ensureStockHistorySeeded(Stock stock) {
        if (stock == null) return;
        long existingCount = stockPriceHistoryRepository.countByStock(stock);
        if (existingCount >= 100) {
            return;
        }

        double curPrice = stock.getPrice();
        double changePercent = stock.getChangePercent();
        double yesterdayPrice = curPrice / (1.0 + (changePercent / 100.0));
        if (yesterdayPrice <= 0) yesterdayPrice = curPrice;

        LocalDate today = LocalDate.now();
        List<StockPriceHistory> toSave = new ArrayList<>();

        // Generate 365 daily candles backwards from yesterday
        double runningPrice = yesterdayPrice;
        List<StockPriceHistory> dailyCandles = new ArrayList<>();

        for (int i = 1; i <= 365; i++) {
            LocalDateTime dayTime = today.minusDays(i).atTime(15, 30);
            double dailyReturn = (ThreadLocalRandom.current().nextDouble() - 0.495) * 0.032;
            double open = Math.round(runningPrice * 100.0) / 100.0;
            double close = Math.max(1.0, Math.round((open * (1.0 + dailyReturn)) * 100.0) / 100.0);
            double high = Math.max(open, close) + (ThreadLocalRandom.current().nextDouble() * (open * 0.012));
            high = Math.round(high * 100.0) / 100.0;
            double low = Math.min(open, close) - (ThreadLocalRandom.current().nextDouble() * (open * 0.012));
            low = Math.max(0.5, Math.round(low * 100.0) / 100.0);
            long volume = ThreadLocalRandom.current().nextLong(150000, 2500000);

            dailyCandles.add(new StockPriceHistory(null, stock, close, dayTime, open, high, low, close, volume));
            runningPrice = close;
        }

        Collections.reverse(dailyCandles);
        toSave.addAll(dailyCandles);

        // Generate 35 intraday bars for today
        double todayOpen = yesterdayPrice * (1.0 + ((ThreadLocalRandom.current().nextDouble() - 0.5) * 0.006));
        double intradayRunning = todayOpen;
        int barCount = 35;

        for (int i = 0; i < barCount; i++) {
            int minutesFrom915 = i * 10;
            int hour = 9 + (15 + minutesFrom915) / 60;
            int minute = (15 + minutesFrom915) % 60;
            if (hour > 15 || (hour == 15 && minute > 30)) {
                hour = 15;
                minute = 30;
            }
            LocalDateTime barTime = today.atTime(hour, minute);

            double target = todayOpen + ((curPrice - todayOpen) * ((double) (i + 1) / barCount));
            double noise = (ThreadLocalRandom.current().nextDouble() - 0.5) * (todayOpen * 0.004);
            double close = (i == barCount - 1) ? curPrice : Math.round((target + noise) * 100.0) / 100.0;
            double open = intradayRunning;
            double high = Math.max(open, close) + (ThreadLocalRandom.current().nextDouble() * (open * 0.003));
            high = Math.round(high * 100.0) / 100.0;
            double low = Math.min(open, close) - (ThreadLocalRandom.current().nextDouble() * (open * 0.003));
            low = Math.max(0.5, Math.round(low * 100.0) / 100.0);
            long volume = ThreadLocalRandom.current().nextLong(5000, 65000);

            toSave.add(new StockPriceHistory(null, stock, close, barTime, open, high, low, close, volume));
            intradayRunning = close;
        }

        stockPriceHistoryRepository.saveAll(toSave);
    }

    public List<StockPriceHistoryDTO> getPriceHistory(String symbol, String range) {
        Optional<Stock> stockOpt = getStockBySymbol(symbol);
        if (stockOpt.isEmpty()) {
            return List.of();
        }

        Stock stock = stockOpt.get();
        ensureStockHistorySeeded(stock);

        String cleanRange = (range != null) ? range.toUpperCase().trim() : "1D";
        LocalDate today = LocalDate.now();

        List<StockPriceHistoryDTO> result = new ArrayList<>();

        if ("1D".equalsIgnoreCase(cleanRange)) {
            LocalDateTime startOfToday = today.atStartOfDay();
            List<StockPriceHistory> todayHistory = stockPriceHistoryRepository.findByStockAndRecordedAtAfterOrderByRecordedAtAsc(stock, startOfToday);

            if (todayHistory.isEmpty()) {
                todayHistory = stockPriceHistoryRepository.findTop40ByStockOrderByRecordedAtDesc(stock);
                Collections.reverse(todayHistory);
            }

            for (StockPriceHistory h : todayHistory) {
                long epochSec = h.getRecordedAt().atZone(ZoneId.systemDefault()).toEpochSecond();
                result.add(new StockPriceHistoryDTO(
                    h.getId(),
                    h.getOpenPrice(),
                    h.getHighPrice(),
                    h.getLowPrice(),
                    h.getClosePrice(),
                    h.getVolume(),
                    String.valueOf(epochSec),
                    h.getRecordedAt()
                ));
            }

            if (!result.isEmpty()) {
                StockPriceHistoryDTO last = result.get(result.size() - 1);
                last.setClose(stock.getPrice());
                last.setPrice(stock.getPrice());
            }
            return result;
        }

        int limitDays = switch (cleanRange) {
            case "1W" -> 7;
            case "1M" -> 30;
            case "3M" -> 90;
            case "1Y" -> 365;
            default -> 1000; // ALL
        };

        LocalDateTime cutoff = today.minusDays(limitDays).atStartOfDay();
        List<StockPriceHistory> history = stockPriceHistoryRepository.findByStockAndRecordedAtAfterOrderByRecordedAtAsc(stock, cutoff);

        // Group into daily points (one candle per calendar date)
        Map<LocalDate, List<StockPriceHistory>> byDate = new LinkedHashMap<>();
        for (StockPriceHistory h : history) {
            LocalDate d = h.getRecordedAt().toLocalDate();
            byDate.computeIfAbsent(d, k -> new ArrayList<>()).add(h);
        }

        for (Map.Entry<LocalDate, List<StockPriceHistory>> entry : byDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<StockPriceHistory> points = entry.getValue();
            if (points.isEmpty()) continue;

            double open = points.get(0).getOpenPrice();
            double close = points.get(points.size() - 1).getClosePrice();
            double high = points.stream().mapToDouble(StockPriceHistory::getHighPrice).max().orElse(Math.max(open, close));
            double low = points.stream().mapToDouble(StockPriceHistory::getLowPrice).min().orElse(Math.min(open, close));
            long volume = points.stream().mapToLong(StockPriceHistory::getVolume).sum();

            result.add(new StockPriceHistoryDTO(
                points.get(0).getId(),
                Math.round(open * 100.0) / 100.0,
                Math.round(high * 100.0) / 100.0,
                Math.round(low * 100.0) / 100.0,
                Math.round(close * 100.0) / 100.0,
                volume,
                date.toString(),
                date.atTime(15, 30)
            ));
        }

        if (!result.isEmpty()) {
            StockPriceHistoryDTO last = result.get(result.size() - 1);
            last.setClose(stock.getPrice());
            last.setPrice(stock.getPrice());
        }

        return result;
    }

    public List<Stock> getLiveStocks() {
        List<Stock> list = stockRepository.findAll();
        populateSparklines(list);
        return list;
    }

    @Scheduled(fixedRate = 4000)
    @Transactional
    public void simulatePriceMovements() {
        List<Stock> stocks = stockRepository.findAll();
        if (stocks.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();

        for (Stock stock : stocks) {
            boolean isEtf = stock.getInstrumentType() == InstrumentType.ETF
                    || "ETF".equalsIgnoreCase(stock.getSector())
                    || "NIFTYBEES".equalsIgnoreCase(stock.getSymbol());

            // Realistic daily move limit: +-1% for ETFs, +-3% for stocks
            double maxBound = isEtf ? 1.0 : 3.0;

            // Small tick move with gentle mean reversion pull if near limit
            double currentChange = stock.getChangePercent();
            double drift = 0.0;
            if (currentChange > (maxBound * 0.70)) {
                drift = -0.03;
            } else if (currentChange < (-maxBound * 0.70)) {
                drift = 0.03;
            }

            double tickSpread = isEtf ? 0.05 : 0.15;
            double tickDelta = ((ThreadLocalRandom.current().nextDouble() - 0.5) * tickSpread) + drift;

            double newChange = currentChange + tickDelta;
            newChange = Math.max(-maxBound, Math.min(maxBound, Math.round(newChange * 100.0) / 100.0));

            double basePrice = stock.getPrice() / (1.0 + (currentChange / 100.0));
            if (basePrice <= 0.01) basePrice = stock.getPrice();

            double newPrice = Math.max(0.50, Math.round(basePrice * (1.0 + (newChange / 100.0)) * 100.0) / 100.0);

            double oldPrice = stock.getPrice();
            stock.setPrice(newPrice);
            stock.setChangePercent(newChange);
            stockRepository.save(stock);

            // Record price history tick periodically (1 in 2 ticks)
            if (ThreadLocalRandom.current().nextInt(2) == 0) {
                double open = oldPrice;
                double close = newPrice;
                double high = Math.max(open, close) + (ThreadLocalRandom.current().nextDouble() * (isEtf ? 0.04 : 0.20));
                double low = Math.min(open, close) - (ThreadLocalRandom.current().nextDouble() * (isEtf ? 0.04 : 0.20));
                long volume = ThreadLocalRandom.current().nextLong(500, 15000);
                stockPriceHistoryRepository.save(new StockPriceHistory(
                    null, stock, newPrice, now,
                    Math.round(open * 100.0) / 100.0,
                    Math.round(high * 100.0) / 100.0,
                    Math.max(0.5, Math.round(low * 100.0) / 100.0),
                    Math.round(close * 100.0) / 100.0,
                    volume
                ));
            }
        }
    }

    public Stock updateStock(Long id, StockRequest request) {
        Stock stock = stockRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Stock not found with ID: " + id));

        if (request.getSymbol() != null && !request.getSymbol().isBlank()) {
            stock.setSymbol(request.getSymbol().toUpperCase().trim());
        }
        if (request.getCompanyName() != null && !request.getCompanyName().isBlank()) {
            stock.setCompanyName(request.getCompanyName().trim());
        }
        if (request.getPrice() > 0) {
            stock.setPrice(request.getPrice());
        }
        stock.setChangePercent(request.getChangePercent());
        if (request.getSector() != null && !request.getSector().isBlank()) {
            stock.setSector(request.getSector().trim());
        }

        return stockRepository.save(stock);
    }

    @Transactional
    public boolean deleteStock(Long id) {
        if (!stockRepository.existsById(id)) {
            return false;
        }
        watchlistRepository.deleteByStockId(id);
        stockPriceHistoryRepository.deleteByStockId(id);
        portfolioRepository.deleteByStockId(id);
        transactionRepository.deleteByStockId(id);
        stockRepository.deleteById(id);
        return true;
    }
}