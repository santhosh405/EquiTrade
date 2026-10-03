package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Stock;
import com.example.demo.model.User;
import com.example.demo.model.Watchlist;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    List<Watchlist> findByUser(User user);

    List<Watchlist> findByUserId(Long userId);

    Optional<Watchlist> findByUserAndStock(User user, Stock stock);

    Optional<Watchlist> findByUserIdAndStockId(Long userId, Long stockId);

    boolean existsByUserIdAndStockId(Long userId, Long stockId);

    void deleteByUserIdAndStockId(Long userId, Long stockId);

    void deleteByStockId(Long stockId);
}
