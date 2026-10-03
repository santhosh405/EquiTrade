package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Stock;
import com.example.demo.model.StockPriceHistory;

@Repository
public interface StockPriceHistoryRepository extends JpaRepository<StockPriceHistory, Long> {

    List<StockPriceHistory> findByStockOrderByRecordedAtAsc(Stock stock);

    List<StockPriceHistory> findByStockIdOrderByRecordedAtAsc(Long stockId);

    List<StockPriceHistory> findByStockAndRecordedAtAfterOrderByRecordedAtAsc(Stock stock, LocalDateTime afterDate);

    List<StockPriceHistory> findByStockAndRecordedAtBetweenOrderByRecordedAtAsc(Stock stock, LocalDateTime start, LocalDateTime end);

    List<StockPriceHistory> findTop40ByStockOrderByRecordedAtDesc(Stock stock);

    long countByStock(Stock stock);

    void deleteByStockId(Long stockId);
}

