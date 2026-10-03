package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Portfolio;
import com.example.demo.model.Stock;
import com.example.demo.model.User;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findByUser(User user);

    List<Portfolio> findByUserId(Long userId);

    Optional<Portfolio> findByUserAndStock(User user, Stock stock);

    List<Portfolio> findByStock(Stock stock);

    void deleteByStockId(Long stockId);
}