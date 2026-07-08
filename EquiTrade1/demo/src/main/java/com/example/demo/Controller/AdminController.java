package com.example.demo.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Stock;
import com.example.demo.repository.StockRepository;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private StockRepository stockRepository;

    @GetMapping("/stocks")
    public List<Stock> getStocks() {
        return stockRepository.findAll();
    }

    @DeleteMapping("/stock/{id}")
    public String deleteStock(@PathVariable Long id) {

        if (stockRepository.existsById(id)) {
            stockRepository.deleteById(id);
            return "Stock Deleted Successfully";
        }

        return "Stock Not Found";
    }
}