package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.TradeRequest;
import com.example.demo.model.Portfolio;
import com.example.demo.model.Stock;
import com.example.demo.model.Transaction;
import com.example.demo.model.User;
import com.example.demo.repository.PortfolioRepository;
import com.example.demo.repository.StockRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.UserRepository;

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

    public String buyStock(TradeRequest request) {

        Optional<User> userOpt = userRepository.findById(request.getUserId());
        Optional<Stock> stockOpt = stockRepository.findById(request.getStockId());

        if (userOpt.isEmpty() || stockOpt.isEmpty()) {
            return "User or Stock not found";
        }

        User user = userOpt.get();
        Stock stock = stockOpt.get();

        double totalPrice = stock.getPrice() * request.getQuantity();

        if (user.getBalance() < totalPrice) {
            return "Insufficient Balance";
        }

        user.setBalance(user.getBalance() - totalPrice);
        userRepository.save(user);

        Portfolio portfolio = new Portfolio();
        portfolio.setUser(user);
        portfolio.setStock(stock);
        portfolio.setQuantity(request.getQuantity());

        portfolioRepository.save(portfolio);

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setStock(stock);
        transaction.setQuantity(request.getQuantity());
        transaction.setPrice(stock.getPrice());
        transaction.setType("BUY");
        transaction.setDate(LocalDateTime.now());

        transactionRepository.save(transaction);

        return "Stock Purchased Successfully";
    }

}