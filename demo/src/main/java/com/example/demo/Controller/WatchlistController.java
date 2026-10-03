package com.example.demo.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.WatchlistDTO;
import com.example.demo.model.User;
import com.example.demo.security.SecurityUtil;
import com.example.demo.service.WatchlistService;

@RestController
@RequestMapping({"/watchlist", "/api/watchlist"})
public class WatchlistController {

    @Autowired
    private WatchlistService watchlistService;

    @Autowired
    private SecurityUtil securityUtil;

    @GetMapping
    public ResponseEntity<List<WatchlistDTO>> getWatchlist() {
        User user = securityUtil.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(watchlistService.getUserWatchlist(user));
    }

    @PostMapping("/{stockId}")
    public ResponseEntity<ApiResponse> addToWatchlist(@PathVariable Long stockId) {
        User user = securityUtil.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, "Unauthorized"));
        return ResponseEntity.ok(watchlistService.addToWatchlist(user, stockId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> addToWatchlistBody(@RequestBody Map<String, Object> body) {
        Object val = body.get("stockId");
        Long stockId = val instanceof Number ? ((Number) val).longValue() : Long.valueOf(String.valueOf(val));
        User user = securityUtil.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, "Unauthorized"));
        return ResponseEntity.ok(watchlistService.addToWatchlist(user, stockId));
    }

    @DeleteMapping("/{stockId}")
    public ResponseEntity<ApiResponse> removeFromWatchlist(@PathVariable Long stockId) {
        User user = securityUtil.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, "Unauthorized"));
        return ResponseEntity.ok(watchlistService.removeFromWatchlist(user, stockId));
    }
}
