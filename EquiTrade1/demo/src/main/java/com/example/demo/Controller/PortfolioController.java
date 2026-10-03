package com.example.demo.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.PortfolioHoldingDTO;
import com.example.demo.dto.PortfolioSummaryDTO;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.SecurityUtil;
import com.example.demo.service.PortfolioService;

@RestController
@RequestMapping({"/portfolio", "/api/portfolio"})
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityUtil securityUtil;

    @GetMapping
    public ResponseEntity<PortfolioSummaryDTO> getPortfolioSummary() {
        User user = securityUtil.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(portfolioService.getPortfolioSummary(user));
    }

    @GetMapping("/summary")
    public ResponseEntity<PortfolioSummaryDTO> getPortfolioSummaryExplicit() {
        User user = securityUtil.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(portfolioService.getPortfolioSummary(user));
    }

    @GetMapping("/holdings")
    public ResponseEntity<List<PortfolioHoldingDTO>> getPortfolioHoldings() {
        User user = securityUtil.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        PortfolioSummaryDTO summary = portfolioService.getPortfolioSummary(user);
        return ResponseEntity.ok(summary != null && summary.getHoldings() != null ? summary.getHoldings() : List.of());
    }

    @GetMapping("/performance")
    public ResponseEntity<com.example.demo.dto.PortfolioPerformanceDTO> getPortfolioPerformance(
            @RequestParam(defaultValue = "1M") String range) {
        User user = securityUtil.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(portfolioService.getPortfolioPerformance(user, range));
    }

    /**
     * GET /portfolio/{userId} — allowed only for the resource owner or an
     * admin-role principal.  Uses an explicit role check rather than
     * role.contains("ADMIN") to avoid substring-matching attacks.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getPortfolio(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Invalid user ID"));
        }

        // Enforce: only the owner or an explicit admin role may read this
        if (!securityUtil.isCurrentUserOrAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Access denied"));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "User not found"));
        }

        return ResponseEntity.ok(portfolioService.getPortfolio(user));
    }
}
