package com.example.demo.Controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.PagedStockResponse;
import com.example.demo.dto.StockPriceHistoryDTO;
import com.example.demo.model.Stock;
import com.example.demo.service.StockService;

@RestController
@RequestMapping({"/stocks", "/api/stocks"})
public class StockController {

    @Autowired
    private StockService stockService;

    @GetMapping
    public List<Stock> getAllStocks() {
        return stockService.getAllStocks();
    }

    @GetMapping("/live")
    public List<Stock> getLiveStocks() {
        return stockService.getLiveStocks();
    }

    @GetMapping("/paged")
    public PagedStockResponse getPagedStocks(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "price") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return stockService.getPagedStocks(query, sector, minPrice, maxPrice, page, size, sortBy, sortDir);
    }

    @GetMapping("/sectors")
    public List<String> getSectors() {
        return stockService.getDistinctSectors();
    }

    @GetMapping("/gainers")
    public List<Stock> getGainers() {
        return stockService.getTopGainers();
    }

    @GetMapping("/losers")
    public List<Stock> getLosers() {
        return stockService.getTopLosers();
    }

    @GetMapping("/search")
    public List<Stock> searchStocks(@RequestParam(required = false, defaultValue = "") String q) {
        return stockService.searchStocks(q);
    }

    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<Stock> getStockBySymbol(@PathVariable String symbol) {
        return stockService.getStockBySymbol(symbol)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Stock> getStockById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }
        return stockService.getStockById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{symbol}/history")
    public List<StockPriceHistoryDTO> getStockHistory(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1D") String range) {
        return stockService.getPriceHistory(symbol, range);
    }

    @PostMapping("/add")
    public ResponseEntity<Stock> addStock(@RequestBody Stock stock) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.addStock(stock));
    }

    @PostMapping
    public ResponseEntity<Stock> createStock(@RequestBody Stock stock) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.addStock(stock));
    }
}