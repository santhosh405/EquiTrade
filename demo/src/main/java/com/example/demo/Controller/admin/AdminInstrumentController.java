package com.example.demo.Controller.admin;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.ApiResponse;
import com.example.demo.model.Stock;
import com.example.demo.model.enums.Exchange;
import com.example.demo.model.enums.InstrumentStatus;
import com.example.demo.model.enums.InstrumentType;
import com.example.demo.repository.StockRepository;
import com.example.demo.service.AuditLogService;
import com.example.demo.service.StockService;

@RestController
@RequestMapping({"/admin/instruments", "/api/admin/instruments"})
public class AdminInstrumentController {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockService stockService;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER')")
    public ResponseEntity<List<Stock>> getInstruments(
            @RequestParam(required = false) String exchange,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {

        List<Stock> list = stockRepository.findAll();
        List<Stock> filtered = list.stream().filter(s -> {
            if (exchange != null && !exchange.isBlank() && (s.getExchange() == null || !s.getExchange().name().equalsIgnoreCase(exchange))) {
                return false;
            }
            if (type != null && !type.isBlank() && (s.getInstrumentType() == null || !s.getInstrumentType().name().equalsIgnoreCase(type))) {
                return false;
            }
            if (status != null && !status.isBlank() && (s.getInstrumentStatus() == null || !s.getInstrumentStatus().name().equalsIgnoreCase(status))) {
                return false;
            }
            return true;
        }).toList();

        return ResponseEntity.ok(filtered);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER')")
    public ResponseEntity<Stock> addInstrument(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String symbol = (String) body.get("symbol");
        String companyName = (String) body.get("companyName");
        String sector = (String) body.get("sector");
        String exchangeStr = (String) body.get("exchange");
        String typeStr = (String) body.get("instrumentType");

        if (symbol == null || symbol.isBlank() || companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("Symbol and company name are required");
        }

        // Price is determined ONLY by the market-data feed service, not manual admin input
        double defaultFeedPrice = 100.0;

        Stock stock = new Stock();
        stock.setSymbol(symbol.toUpperCase().trim());
        stock.setCompanyName(companyName.trim());
        stock.setSector(sector != null && !sector.isBlank() ? sector.trim() : "General");
        stock.setPrice(defaultFeedPrice);
        stock.setChangePercent(0.0);
        stock.setExchange(exchangeStr != null ? Exchange.valueOf(exchangeStr.toUpperCase()) : Exchange.NSE);
        stock.setInstrumentType(typeStr != null ? InstrumentType.valueOf(typeStr.toUpperCase()) : InstrumentType.EQUITY);
        stock.setInstrumentStatus(InstrumentStatus.ACTIVE);

        if (body.get("lotSize") != null) {
            stock.setLotSize(((Number) body.get("lotSize")).intValue());
        }
        if (body.get("tickSize") != null) {
            stock.setTickSize(((Number) body.get("tickSize")).doubleValue());
        }

        Stock created = stockService.addStock(stock);

        auditLogService.logAction("CREATE_INSTRUMENT", "STOCK", created.getSymbol(),
                null, "Created listing " + created.getSymbol(), "Instrument registered", request);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER')")
    public ResponseEntity<Stock> updateInstrument(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with ID: " + id));

        String before = stock.getSymbol() + " / " + stock.getInstrumentStatus() + " / restricted=" + stock.isTradingRestricted();

        if (body.containsKey("companyName")) {
            stock.setCompanyName((String) body.get("companyName"));
        }
        if (body.containsKey("sector")) {
            stock.setSector((String) body.get("sector"));
        }
        if (body.containsKey("exchange")) {
            stock.setExchange(Exchange.valueOf(((String) body.get("exchange")).toUpperCase()));
        }
        if (body.containsKey("instrumentType")) {
            stock.setInstrumentType(InstrumentType.valueOf(((String) body.get("instrumentType")).toUpperCase()));
        }
        if (body.containsKey("instrumentStatus")) {
            stock.setInstrumentStatus(InstrumentStatus.valueOf(((String) body.get("instrumentStatus")).toUpperCase()));
        }
        if (body.containsKey("lotSize")) {
            stock.setLotSize(((Number) body.get("lotSize")).intValue());
        }
        if (body.containsKey("tickSize")) {
            stock.setTickSize(((Number) body.get("tickSize")).doubleValue());
        }
        // Note: Admin can NOT modify market price directly! Prices come only from feed.

        Stock updated = stockRepository.save(stock);

        auditLogService.logAction("UPDATE_INSTRUMENT", "STOCK", updated.getSymbol(),
                before, updated.getSymbol() + " / " + updated.getInstrumentStatus(), "Instrument updated", request);

        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/restriction")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'RISK_OFFICER')")
    public ResponseEntity<ApiResponse> toggleTradingRestriction(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String reason = (String) body.get("reason");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A mandatory reason must be provided to modify trading restrictions");
        }

        boolean restricted = Boolean.parseBoolean(String.valueOf(body.getOrDefault("restricted", "true")));

        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with ID: " + id));

        boolean before = stock.isTradingRestricted();
        stock.setTradingRestricted(restricted);
        stock.setRestrictionReason(restricted ? reason : null);
        stockRepository.save(stock);

        auditLogService.logAction("TOGGLE_TRADING_RESTRICTION", "STOCK", stock.getSymbol(),
                String.valueOf(before), String.valueOf(restricted), reason, request);

        return ResponseEntity.ok(new ApiResponse(true,
                restricted ? "Trading restricted for " + stock.getSymbol() : "Trading restrictions lifted for " + stock.getSymbol()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse> deleteInstrument(@PathVariable Long id, HttpServletRequest request) {
        Stock stock = stockRepository.findById(id).orElse(null);
        if (stock == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, "Stock not found"));
        }

        boolean deleted = stockService.deleteStock(id);
        if (deleted) {
            auditLogService.logAction("DELETE_INSTRUMENT", "STOCK", stock.getSymbol(),
                    stock.getSymbol(), "DELETED", "Listing removed from exchange", request);
            return ResponseEntity.ok(new ApiResponse(true, "Instrument deleted successfully"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, "Cannot delete instrument with existing trades/holdings. Consider setting status to DELISTED."));
    }
}
