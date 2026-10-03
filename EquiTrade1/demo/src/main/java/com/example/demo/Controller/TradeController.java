package com.example.demo.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.TradeRequest;
import com.example.demo.dto.TransactionDTO;
import com.example.demo.model.User;
import com.example.demo.security.SecurityUtil;
import com.example.demo.service.TradeService;

@RestController
@RequestMapping({"/trade", "/api/trade"})
public class TradeController {

    @Autowired
    private TradeService tradeService;

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping("/buy")
    public ResponseEntity<ApiResponse> buyStock(@RequestBody TradeRequest request) {
        String result = tradeService.buyStock(request);
        if ("Stock Purchased Successfully".equalsIgnoreCase(result)) {
            // Re-read balance from JWT principal (never from request.userId)
            User user = securityUtil.getCurrentUser();
            Double bal = user != null ? user.getBalance() : null;
            return ResponseEntity.ok(new ApiResponse(true, result, bal));
        } else if ("Stock not found".equalsIgnoreCase(result) || "User not found".equalsIgnoreCase(result)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, result));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, result));
        }
    }

    @PostMapping("/sell")
    public ResponseEntity<ApiResponse> sellStock(@RequestBody TradeRequest request) {
        String result = tradeService.sellStock(request);
        if ("Stock Sold Successfully".equalsIgnoreCase(result)) {
            // Re-read balance from JWT principal (never from request.userId)
            User user = securityUtil.getCurrentUser();
            Double bal = user != null ? user.getBalance() : null;
            return ResponseEntity.ok(new ApiResponse(true, result, bal));
        } else if ("Stock not found".equalsIgnoreCase(result) || "User not found".equalsIgnoreCase(result)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, result));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, result));
        }
    }

    @GetMapping("/history")
    public List<TransactionDTO> getHistory() {
        // Always history for the current JWT user only
        User user = securityUtil.getCurrentUser();
        return tradeService.getHistory(user);
    }
}