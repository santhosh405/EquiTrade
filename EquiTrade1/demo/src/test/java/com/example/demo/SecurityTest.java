package com.example.demo;

import com.example.demo.config.DataInitializer;
import com.example.demo.model.Stock;
import com.example.demo.model.User;
import com.example.demo.model.enums.AccessStatus;
import com.example.demo.model.enums.AccountStatus;
import com.example.demo.model.enums.KycStatus;
import com.example.demo.repository.PortfolioRepository;
import com.example.demo.repository.StockPriceHistoryRepository;
import com.example.demo.repository.StockRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.UserLoginHistoryRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WalletTransactionRepository;
import com.example.demo.repository.WatchlistRepository;
import com.example.demo.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security integration tests — verifies the five key hardening requirements:
 *
 *  1. "Bearer Token-<email>" mock-backdoor tokens return 401
 *  2. Unauthenticated POST /trade/buy returns 401
 *  3. User A cannot trade on behalf of User B (IDOR)
 *  4. User A cannot read User B's portfolio (IDOR)
 *  5. A normal ROLE_USER gets 403 on /admin/**
 *  6. Anonymous POST /stocks (create) returns 401 or 403
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    // Child tables that FK into stocks or users — must be cleared before deleting parents
    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private StockPriceHistoryRepository stockPriceHistoryRepository;

    @Autowired
    private UserLoginHistoryRepository userLoginHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    // Suppress the heavy DataInitializer so we fully control test data
    @MockBean
    private DataInitializer dataInitializer;

    private User userA;
    private User userB;
    private Stock stock;

    @BeforeEach
    void setup() {
        // Delete child rows first (FK constraints), then parents
        walletTransactionRepository.deleteAll();
        transactionRepository.deleteAll();
        portfolioRepository.deleteAll();
        watchlistRepository.deleteAll();
        stockPriceHistoryRepository.deleteAll();
        userLoginHistoryRepository.deleteAll();
        stockRepository.deleteAll();
        userRepository.deleteAll();

        userA = new User();
        userA.setUsername("userA");
        userA.setEmail("usera@test.com");
        userA.setPassword(passwordEncoder.encode("PasswordA1!"));
        userA.setRole("ROLE_USER");
        userA.setAccessStatus(AccessStatus.APPROVED);
        userA.setKycStatus(KycStatus.APPROVED);
        userA.setAccountStatus(AccountStatus.ACTIVE);
        userA.setBalance(500_000.0);
        userA = userRepository.save(userA);

        userB = new User();
        userB.setUsername("userB");
        userB.setEmail("userb@test.com");
        userB.setPassword(passwordEncoder.encode("PasswordB1!"));
        userB.setRole("ROLE_USER");
        userB.setAccessStatus(AccessStatus.APPROVED);
        userB.setKycStatus(KycStatus.APPROVED);
        userB.setAccountStatus(AccountStatus.ACTIVE);
        userB.setBalance(500_000.0);
        userB = userRepository.save(userB);

        stock = new Stock();
        stock.setSymbol("TST");
        stock.setCompanyName("Test Corp");
        stock.setPrice(100.0);
        stock.setChangePercent(0.0);
        stock.setSector("Test");
        stock.setInstrumentStatus(com.example.demo.model.enums.InstrumentStatus.ACTIVE);
        stock.setExchange(com.example.demo.model.enums.Exchange.NSE);
        stock.setInstrumentType(com.example.demo.model.enums.InstrumentType.EQUITY);
        stock = stockRepository.save(stock);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String bearerFor(User user) {
        return "Bearer " + jwtUtils.generateToken(user.getEmail());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: Mock-backdoor "Token-" format must be rejected with 401
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Bearer Token-<email> backdoor token → 401")
    void mockBackdoorToken_returns401() throws Exception {
        mockMvc.perform(get("/trade/history")
                        .header("Authorization", "Bearer Token-admin@equitrade.com"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Bearer Token-superadmin@equitrade.com backdoor → 401 on admin endpoint")
    void mockBackdoorTokenAdmin_returns401() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .header("Authorization", "Bearer Token-superadmin@equitrade.com"))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: Unauthenticated POST /trade/buy → 401
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Unauthenticated POST /trade/buy → 401")
    void unauthenticated_buyStock_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("stockId", stock.getId(), "quantity", 1));

        mockMvc.perform(post("/trade/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated POST /trade/sell → 401")
    void unauthenticated_sellStock_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("stockId", stock.getId(), "quantity", 1));

        mockMvc.perform(post("/trade/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: IDOR — User A cannot trade as User B by sending B's userId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("User A supplying User B's userId in buy request still trades as User A (IDOR blocked)")
    void buyStock_withForeignUserId_usesJwtOwner() throws Exception {
        // Include userB's id in the payload — should be silently ignored
        String body = objectMapper.writeValueAsString(
                Map.of("userId", userB.getId(), "stockId", stock.getId(), "quantity", 1));

        mockMvc.perform(post("/trade/buy")
                        .header("Authorization", bearerFor(userA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // userB's balance must be unchanged
        User freshB = userRepository.findById(userB.getId()).orElseThrow();
        assert freshB.getBalance() == 500_000.0
                : "IDOR: userB balance was wrongly modified to " + freshB.getBalance();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4: IDOR — User A cannot read User B's portfolio
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("User A cannot read User B's portfolio → 403")
    void userA_cannotRead_userBPortfolio() throws Exception {
        mockMvc.perform(get("/portfolio/" + userB.getId())
                        .header("Authorization", bearerFor(userA)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User A can read their own portfolio → 200")
    void userA_canRead_ownPortfolio() throws Exception {
        mockMvc.perform(get("/portfolio/" + userA.getId())
                        .header("Authorization", bearerFor(userA)))
                .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5: Normal user gets 403 on /admin/**
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ROLE_USER gets 403 on GET /admin/users")
    void normalUser_gets403_onAdminEndpoint() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .header("Authorization", bearerFor(userA)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous user gets 401 on GET /admin/users")
    void anonymous_gets401_onAdminEndpoint() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 6: Anonymous POST /stocks → 401; normal user POST /stocks → 403
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Anonymous POST /stocks/add → 401")
    void anonymous_postStocks_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("symbol", "HACK", "companyName", "Hack Corp", "price", 1.0));

        mockMvc.perform(post("/stocks/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Normal ROLE_USER POST /stocks/add → 403")
    void normalUser_postStocks_returns403() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("symbol", "HACK", "companyName", "Hack Corp", "price", 1.0));

        mockMvc.perform(post("/stocks/add")
                        .header("Authorization", bearerFor(userA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 7: /auth/me does not expose password / sensitive fields
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("/auth/me response does not expose password, failedLoginAttempts, panNumber, maskedAadhaar")
    void authMe_doesNotExposePassword() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", bearerFor(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.failedLoginAttempts").doesNotExist())
                .andExpect(jsonPath("$.panNumber").doesNotExist())
                .andExpect(jsonPath("$.maskedAadhaar").doesNotExist())
                .andExpect(jsonPath("$.lockedUntil").doesNotExist())
                .andExpect(jsonPath("$.email").value("usera@test.com"));
    }

    @Test
    @DisplayName("Unauthenticated GET /auth/me → 401")
    void unauthenticated_authMe_returns401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
