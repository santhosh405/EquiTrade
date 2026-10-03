package com.example.demo.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.WatchlistDTO;
import com.example.demo.model.Stock;
import com.example.demo.model.User;
import com.example.demo.model.Watchlist;
import com.example.demo.repository.StockRepository;
import com.example.demo.repository.WatchlistRepository;

@Service
public class WatchlistService {

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private StockRepository stockRepository;

    public List<WatchlistDTO> getUserWatchlist(User user) {
        if (user == null) {
            return List.of();
        }

        List<Watchlist> items = watchlistRepository.findByUser(user);
        return items.stream().map(w -> {
            Stock s = w.getStock();
            return new WatchlistDTO(
                w.getId(),
                s.getId(),
                s.getSymbol(),
                s.getCompanyName(),
                s.getPrice(),
                s.getChangePercent(),
                s.getSector()
            );
        }).collect(Collectors.toList());
    }

    @Transactional
    public ApiResponse addToWatchlist(User user, Long stockId) {
        if (user == null) {
            throw new IllegalArgumentException("User authentication required");
        }

        Stock stock = stockRepository.findById(stockId)
            .orElseThrow(() -> new IllegalArgumentException("Stock not found with id: " + stockId));

        if (watchlistRepository.existsByUserIdAndStockId(user.getId(), stockId)) {
            return new ApiResponse(true, "Stock already in watchlist");
        }

        Watchlist watchlist = new Watchlist(user, stock);
        watchlistRepository.save(watchlist);
        return new ApiResponse(true, "Stock added to watchlist");
    }

    @Transactional
    public ApiResponse removeFromWatchlist(User user, Long stockId) {
        if (user == null) {
            throw new IllegalArgumentException("User authentication required");
        }

        Optional<Watchlist> itemOpt = watchlistRepository.findByUserIdAndStockId(user.getId(), stockId);
        if (itemOpt.isPresent()) {
            watchlistRepository.delete(itemOpt.get());
            return new ApiResponse(true, "Stock removed from watchlist");
        }

        return new ApiResponse(true, "Stock not in watchlist");
    }
}
