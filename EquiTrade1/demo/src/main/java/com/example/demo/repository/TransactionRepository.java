package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Transaction;
import com.example.demo.model.User;
import com.example.demo.model.enums.OrderStatus;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUser(User user);

    List<Transaction> findByUserOrderByDateDesc(User user);

    List<Transaction> findAllByOrderByDateDesc();

    Page<Transaction> findAllByOrderByDateDesc(Pageable pageable);

    List<Transaction> findByStatusOrderByDateDesc(OrderStatus status);

    void deleteByStockId(Long stockId);

    @Query("SELECT t.stock.id, t.stock.symbol, t.stock.companyName, COUNT(t) FROM Transaction t GROUP BY t.stock.id, t.stock.symbol, t.stock.companyName ORDER BY COUNT(t) DESC")
    List<Object[]> findMostTradedStocks();

    long countByDateAfter(LocalDateTime date);
}