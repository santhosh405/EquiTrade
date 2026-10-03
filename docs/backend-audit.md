# EquiTrade Spring Boot Backend — Senior Engineering Audit Report

**Auditor:** Principal Software & Systems Security Reviewer  
**Repository:** `EquiTrade` (Spring Boot 3.5.5 / Java 25)  
**Audit Date:** October 2026  
**Status:** COMPLETE (Zero Code Changes Applied; Recommendations Ready for Implementation)  

---

## Executive Summary & Scorecard

An exhaustive architectural, security, correctness, and operational audit was conducted on the EquiTrade Spring Boot backend (`com.example.demo`). The backend demonstrates an impressive breadth of financial features (simulated order matching, multi-role admin controls, audit logging, statutory charges, risk telemetry, and KYC workflows). However, beneath the feature layer lie **critical security vulnerabilities, severe financial precision defects, race conditions in order execution, and production readiness gaps** that would cause catastrophic failures in a regulated financial environment.

### Audit Scorecard

| Area | Score (out of 10) | Rating | Key Finding |
|---|:---:|:---:|---|
| **1. Security** | **2.5 / 10** | **CRITICAL RISK** | Authentication backdoor (`Token-` prefix), `.anyRequest().permitAll()` leaving user endpoints exposed, unauthenticated stock creation, IDOR in portfolio & trade execution, wildcard CORS with credentials, hardcoded JWT secrets, and reset of user credentials on reboot. |
| **2. Correctness of Trading Logic** | **4.0 / 10** | **HIGH RISK** | Financial math uses `double` with IEEE 754 precision loss instead of `BigDecimal`; zero optimistic (`@Version`) or pessimistic database row locking, allowing race conditions (double-spend / overselling). |
| **3. Validation & Errors** | **4.5 / 10** | **HIGH RISK** | Missing `@Valid` on controllers (`TradeRequest`, `RegisterRequest`, `StockRequest`); services return strings instead of throwing typed domain exceptions; generic exception handler leaks raw exception messages. |
| **4. Design & Architecture** | **4.0 / 10** | **HIGH RISK** | Ubiquitous field injection (`@Autowired`); raw JPA entities leaked in API responses; N+1 queries via `FetchType.EAGER`; heavy in-memory streaming of full tables (`findAll()`) causing OOM hazards; unpaginated history/search endpoints. |
| **5. Database & Persistence** | **3.0 / 10** | **CRITICAL RISK** | Volatile in-memory H2 database (`jdbc:h2:mem:equitrade`); zero schema migration tooling (no Flyway or Liquibase); `DataInitializer` overwrites user balances and passwords on every restart. |
| **6. Automated Tests** | **1.0 / 10** | **CRITICAL RISK** | Exactly 1 test exists (`DemoApplicationTests.contextLoads()`); zero unit or integration tests for trading, risk, portfolio math, or authorization. |
| **7. Operations & Observability** | **3.5 / 10** | **HIGH RISK** | Spring Boot Actuator completely absent; no health/readiness endpoints; no structured JSON logging or request correlation IDs; fallback configuration relies on hardcoded secrets. |
| **OVERALL SYSTEM SCORE** | **3.2 / 10** | **REJECTED FOR PRODUCTION** | **Immediate remediation required before any real-world pilot or deployment.** |

---

## 1. Security Audit

**Score: 2.5 / 10** | **Verdict: Critical Vulnerabilities Present**

### 1.1 Authentication & JWT
* **Hardcoded Secret Key:**  
  * **Evidence:** [JwtUtils.java#L18-L19](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/JwtUtils.java#L18-L19) and [application.properties#L29](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/resources/application.properties#L29)  
  * **Finding:** Fallback secret is statically hardcoded in code and properties: `"EquiTradeSuperSecureSecretKeyThatIsAtLeast32BytesLongForHmacSha256!"`. If the environment variable `JWT_SECRET` is unset, any attacker with access to the repo can mint valid JWT tokens for any user or admin.
  * **Short Key Padding Hazard:** [JwtUtils.java#L26-L32](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/JwtUtils.java#L26-L32) zero-pads keys under 32 bytes rather than failing fast at application boot.
* **Authentication Bypass / Mock Token Backdoor:**  
  * **Evidence:** [JwtUtils.java#L68-L70](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/JwtUtils.java#L68-L70), [JwtUtils.java#L89-L91](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/JwtUtils.java#L89-L91), and [AuthTokenFilter.java#L43-L69](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/AuthTokenFilter.java#L43-L69)  
  * **Finding:** Passing `Authorization: Bearer Token-admin@equitrade.com` bypasses JWT verification entirely. `validateToken` returns `true`, and `getEmailFromToken` extracts the email directly. This grants instant unauthenticated `ROLE_SUPER_ADMIN` privileges.
* **Token Expiration & Revocation:**  
  * **Evidence:** [JwtUtils.java#L21-L22](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/JwtUtils.java#L21-L22)  
  * **Finding:** Default expiration is 1 hour (`3600000 ms`). There is no refresh token rotation, no token revocation list/blacklist on logout, and no check on `User.tokenInvalidatedBefore` or password change events.

### 1.2 Password Hashing
* **Evidence:** [SecurityConfig.java#L29-L31](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/SecurityConfig.java#L29-L31) and [AuthService.java#L134-L136](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/AuthService.java#L134-L136)  
* **Finding:** While `BCryptPasswordEncoder` is declared, `AuthService.java` still permits plain-text password fallback:
  ```java
  boolean matches = passwordEncoder.matches(request.getPassword(), user.getPassword()) ||
                    request.getPassword().equals(user.getPassword());
  ```
  Plain-text comparison allows database-seeded or unhashed passwords to authenticate, creating a false sense of security.

### 1.3 Authorization, Role Checks & Filter Configuration
* **Unprotected Endpoints at Security Filter Level:**  
  * **Evidence:** [SecurityConfig.java#L56-L60](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/SecurityConfig.java#L56-L60)  
  * **Finding:** Line 59 specifies `.anyRequest().permitAll()`. Only `/admin/**` and `/api/admin/**` require roles. Critical endpoints (`/trade/**`, `/portfolio/**`, `/watchlist/**`) are permitted through the security filter without an authenticated principal!
* **Unauthenticated Stock Creation:**  
  * **Evidence:** [SecurityConfig.java#L53-L55](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/SecurityConfig.java#L53-L55) and [StockController.java#L90-L98](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/StockController.java#L90-L98)  
  * **Finding:** `/stocks/**` and `/api/stocks/**` are in `permitAll()`. `StockController` exposes `POST /stocks/add` and `POST /stocks` with no `@PreAuthorize` or authentication check. Any anonymous user on the internet can inject arbitrary stocks into the trading simulator.
* **Public Admin HTML Pages:**  
  * **Evidence:** [SecurityConfig.java#L46](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/SecurityConfig.java#L46)  
  * **Finding:** `/admin/*.html` is explicitly marked `permitAll()`. The frontend administrative UI is completely exposed to unauthenticated users.

### 1.4 Insecure Direct Object Reference (IDOR) & User Resolution
* **Portfolio IDOR:**  
  * **Evidence:** [PortfolioController.java#L61-L71](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/PortfolioController.java#L61-L71)  
  * **Finding:** `GET /portfolio/{userId}` allows any caller to pass any user ID and retrieve that user's entire portfolio without verifying ownership or checking for admin privileges.
* **Trade Execution IDOR:**  
  * **Evidence:** [TradeController.java#L28-L33](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/TradeController.java#L28-L33), [TradeController.java#L42-L47](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/TradeController.java#L42-L47), [TradeService.java#L77](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L77), and [TradeService.java#L191](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L191)  
  * **Finding:** `buyStock` and `sellStock` accept `TradeRequest.userId`. In `TradeService`, it calls `securityUtil.resolveUser(request.getUserId())`. An attacker can specify another user's `userId`, spending the victim's cash balance to buy stocks or dumping the victim's holdings!
* **Catastrophic Anonymous Fallback in SecurityUtil:**  
  * **Evidence:** [SecurityUtil.java#L31-L46](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/SecurityUtil.java#L31-L46)  
  * **Finding:**
    ```java
    public User resolveUser(Long requestedUserId) {
        if (requestedUserId != null) {
            Optional<User> userOpt = userRepository.findById(requestedUserId);
            if (userOpt.isPresent()) return userOpt.get(); // <-- IDOR: accepts ANY requested ID!
        }
        User current = getCurrentUser();
        if (current != null) return current;
        // Fallback for unauthenticated requests:
        return userRepository.findAll().stream()
            .filter(u -> !"ROLE_ADMIN".equals(u.getRole()))
            .findFirst()
            .orElse(...);
    }
    ```
    If no token is supplied, `resolveUser(null)` automatically assumes the identity of the first non-admin user in the database! An unauthenticated user viewing `/watchlist` or `/portfolio` modifies or views that victim's account data.

### 1.5 CORS & Headers Configuration
* **Permissive Origin Pattern with Credentials:**  
  * **Evidence:** [SecurityConfig.java#L70-L75](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/SecurityConfig.java#L70-L75)  
  * **Finding:** `setAllowedOriginPatterns(List.of("*"))` combined with `setAllowCredentials(true)` is an insecure configuration that enables credentialed cross-origin requests from any domain.
* **Frame Options Disabled (Clickjacking):**  
  * **Evidence:** [SecurityConfig.java#L40](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/SecurityConfig.java#L40)  
  * **Finding:** `.headers(headers -> headers.frameOptions(frame -> frame.disable()))` completely disables `X-Frame-Options` globally (done to accommodate H2 console), exposing all trading views to clickjacking attacks.

### 1.6 Hardcoded Secrets & Sensitive Data Exposure
* **Database & Mail Credentials:**  
  * **Evidence:** [application.properties#L6](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/resources/application.properties#L6) and [application.properties#L18-L19](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/resources/application.properties#L18-L19)  
  * **Finding:** Statically defines `spring.datasource.password=123` and default fallback `MAIL_PASSWORD:secret`.
* **Seed User Credential Exposure:**  
  * **Evidence:** [DataInitializer.java#L83-L102](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/config/DataInitializer.java#L83-L102)  
  * **Finding:** Plaintext passwords (`superadmin123`, `admin123`, `kyc123`, `trader123`, `demo123`) are hardcoded in source code.
* **PII & Internal State Leaked in Responses:**  
  * **Evidence:** [AuthController.java#L50](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/AuthController.java#L50) and [AdminUserController.java#L90](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/admin/AdminUserController.java#L90)  
  * **Finding:** Returning `User` entities exposes internal fields (`failedLoginAttempts`, `lockedUntil`, `riskCategory`, `panNumber`, `maskedAadhaar`, `declineReason`).

### 1.7 Rate Limiting on Login
* **Evidence:** [LoginRateLimiter.java#L13-L57](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/LoginRateLimiter.java#L13-L57) and [AuthService.java#L118-L121](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/AuthService.java#L118-L121)  
* **Finding:**
  1. Rate limiting is keyed **only on email**. No IP-based rate limiting exists. An attacker can run credential stuffing across 10,000 distinct accounts without hitting rate limits.
  2. Targeted Denial of Service: An attacker can intentionally trigger 5 failed logins for a victim's email, locking them out for 15 minutes.
  3. Memory leak: `ConcurrentHashMap` has no maximum size or eviction policy for expired keys. Random email spam will exhaust JVM heap memory.

---

## 2. Correctness of Trading Logic

**Score: 4.0 / 10** | **Verdict: High Financial & Concurrency Risk**

### 2.1 Pricing Integrity (Server vs. Client)
* **Evidence:** [TradeService.java#L101](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L101), [TradeService.java#L135](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L135), [TradeService.java#L240](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L240), [TradeService.java#L252](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L252)  
* **Finding:** **PASS.** Market trades strictly execute at `stock.getPrice()`. Client-sent price in `TradeRequest` is not used for execution.

### 2.2 Balance, Quantity & Overselling Checks
* **Evidence:** [TradeService.java#L73-L75](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L73-L75), [TradeService.java#L105-L107](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L105-L107), [TradeService.java#L172-L174](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L172-L174), [TradeService.java#L221-L229](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L221-L229)  
* **Finding:**
  1. Quantity validation properly checks for `<= 0`.
  2. Buy order checks available balance including statutory fee breakdown.
  3. Sell order checks that holding exists and owned shares `>= request.getQuantity()`.
  4. **Integer Overflow Risk:** [TradeService.java#L120](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L120): `int newQty = oldQty + request.getQuantity();` does not guard against `Integer.MAX_VALUE` overflow.

### 2.3 Financial Math: Primitive `double` vs. `BigDecimal`
* **Evidence:** 
  * [User.java#L30](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/model/User.java#L30): `private double balance = 100000.0;`
  * [Stock.java#L27](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/model/Stock.java#L27): `private double price;`
  * [Portfolio.java#L21](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/model/Portfolio.java#L21): `private double averageBuyPrice;`
  * [Transaction.java#L24](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/model/Transaction.java#L24): `private double price;`, `double brokerageFee;`, `double taxes;`, `double totalAmount;`
  * [TradeService.java#L101-L123](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L101-L123) and [TradeService.java#L240-L245](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L240-L245):
    ```java
    user.setBalance(Math.round((user.getBalance() - totalRequired) * 100.0) / 100.0);
    double newAvg = ((oldQty * oldAvg) + grossPrice) / newQty;
    portfolio.setAverageBuyPrice(Math.round(newAvg * 100.0) / 100.0);
    ```
* **Finding:** **CRITICAL DEFECT.** Using primitive `double` and binary floating-point representation (`Math.round(...) / 100.0`) in a trading engine violates financial system engineering principles. IEEE 754 precision issues introduce cumulative rounding errors, fractional cent discrepancies, and ledger drift. All money, price, fee, and balance fields must use `BigDecimal` with explicit scale and rounding mode (e.g., `RoundingMode.HALF_UP`).

### 2.4 Atomicity & Concurrency Protection
* **Atomicity:**  
  * **Evidence:** [TradeService.java#L64](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L64) and [TradeService.java#L167](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L167)  
  * **Finding:** **PASS.** `@Transactional` is present on `buyStock` and `sellStock`. Database updates to `User`, `Portfolio`, `Transaction`, and `WalletTransaction` commit atomically.
* **Concurrency & Double-Click Vulnerability (Zero Locking):**  
  * **Evidence:** [User.java](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/model/User.java), [Portfolio.java](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/model/Portfolio.java), [TradeService.java#L105-L111](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L105-L111), [TradeService.java#L221-L238](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L221-L238)  
  * **Finding:** **CRITICAL DEFECT.**
    * No optimistic locking: Neither `User` nor `Portfolio` declares a `@Version private Long version;` column.
    * No pessimistic locking: `UserRepository` and `PortfolioRepository` do not use `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
    * **Race Condition:** If a user double-clicks "Buy" within milliseconds:
      1. Thread A reads user balance = ₹10,000.
      2. Thread B reads user balance = ₹10,000.
      3. Thread A executes ₹10,000 trade, writes balance = ₹0.
      4. Thread B executes ₹10,000 trade, overwrites balance = ₹0.
      5. The user bought ₹20,000 of stock with only ₹10,000 capital (double-spend).
    * Identical race condition exists on `sellStock`, allowing overselling beyond owned shares.

---

## 3. Validation and Error Handling

**Score: 4.5 / 10** | **Verdict: Incomplete Validation & String-Based Error Coupling**

### 3.1 Bean Validation (`@Valid`) Missing on Controllers
* **Evidence:**
  * [TradeController.java#L28](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/TradeController.java#L28): `buyStock(@RequestBody TradeRequest request)` — missing `@Valid`
  * [TradeController.java#L42](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/TradeController.java#L42): `sellStock(@RequestBody TradeRequest request)` — missing `@Valid`
  * [AuthController.java#L29](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/AuthController.java#L29): `register(@RequestBody RegisterRequest request)` — missing `@Valid`
  * [AuthController.java#L39](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/AuthController.java#L39): `login(@RequestBody LoginRequest request)` — missing `@Valid`
  * [AdminController.java#L110](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/AdminController.java#L110): `addStock(@RequestBody StockRequest request)` — missing `@Valid`
* **Finding:** While `spring-boot-starter-validation` is present in `pom.xml`, controllers do not annotate `@RequestBody` parameters with `@Valid`. Request DTO fields (e.g., `@NotNull`, `@Min(1)`, `@Email`, `@NotBlank`) are never validated by Spring's validation pipeline prior to method execution.

### 3.2 Error Flow: String Return Types vs. Typed Exceptions
* **Evidence:** [TradeController.java#L29-L38](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/TradeController.java#L29-L38) and [TradeService.java#L65-L165](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L65-L165)  
* **Finding:** Service methods return human-readable `String` messages (e.g., `"Stock Purchased Successfully"`, `"Insufficient Balance..."`, `"Stock not found"`), and controllers parse strings using `.equalsIgnoreCase(...)` to guess the appropriate HTTP status code:
  ```java
  String result = tradeService.buyStock(request);
  if ("Stock Purchased Successfully".equalsIgnoreCase(result)) { ... }
  else if ("Stock not found".equalsIgnoreCase(result) || "User not found".equalsIgnoreCase(result)) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)...;
  }
  ```
  This violates clean architecture. Services should throw dedicated exceptions (`InsufficientFundsException`, `ResourceNotFoundException`, `TradingRestrictedException`) which are caught by the `@RestControllerAdvice`.

### 3.3 Global Exception Handling & Information Leakage
* **Evidence:** [GlobalExceptionHandler.java#L95-L103](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/exception/GlobalExceptionHandler.java#L95-L103)  
* **Finding:**
  1. Generic exception handler passes `ex.getMessage()` directly to the client:
     ```java
     @ExceptionHandler(Exception.class)
     public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
         ...
         body.put("message", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
     }
     ```
     Internal SQL syntax errors, table names, constraint names, or NullPointerExceptions leak to callers in production responses.
  2. Missing handlers for:
     * `NoSuchElementException` / `ResourceNotFoundException` (should return 404).
     * `OptimisticLockingFailureException` / `DataIntegrityViolationException` (should return 409 Conflict).
     * `HttpMessageNotReadableException` (malformed JSON should return standard 400).

---

## 4. Design and Architecture

**Score: 4.0 / 10** | **Verdict: High Coupling, Field Injection & Performance Bottlenecks**

### 4.1 Direct Entity Exposure (DTO Violations)
* **Evidence:**
  * [PortfolioController.java#L70](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/PortfolioController.java#L70): Returns `List<Portfolio>` raw entities.
  * [AuthController.java#L50](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/AuthController.java#L50): `/auth/me` returns `User` entity directly.
  * [StockController.java#L24-L98](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/StockController.java#L24-L98): `getAllStocks`, `getLiveStocks`, `getStockById`, `addStock` directly accept and return `Stock` JPA entities.
  * [AdminUserController.java#L90](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/admin/AdminUserController.java#L90) & [L104](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/admin/AdminUserController.java#L104): Returns raw `User` entity.
  * [AdminController.java#L104-L120](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/AdminController.java#L104-L120): Returns `Stock` entities.
* **Finding:** Exposing JPA entities leaks persistence details, causes circular serialization hazards, breaks schema abstraction, and exposes database columns directly to API clients.

### 4.2 Ubiquitous Field Injection (`@Autowired`)
* **Evidence:** Across every single Controller and Service in the project:
  * [TradeController.java#L21-L25](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/TradeController.java#L21-L25)
  * [TradeService.java#L37-L63](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java#L37-L63)
  * [PortfolioController.java#L23-L30](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/PortfolioController.java#L23-L30)
  * [AuthController.java#L22-L26](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/AuthController.java#L22-L26)
  * [AdminController.java#L30-L46](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/AdminController.java#L30-L46)
* **Finding:** Every dependency is injected via reflection into private fields using `@Autowired`. This prevents immutability (`final` fields), impairs isolated unit testing without Spring context, and hides bloated dependency graphs. All components should use explicit **Constructor Injection** (or Lombok `@RequiredArgsConstructor`).

### 4.3 Layering Violations & In-Memory Full Table Scans
* **In-Memory Volume Aggregation (OOM Hazard):**  
  * **Evidence:** [AdminController.java#L54-L56](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/AdminController.java#L54-L56)  
  * **Finding:**
    ```java
    double totalVolume = transactionRepository.findAll().stream()
            .mapToDouble(t -> t.getQuantity() * t.getPrice())
            .sum();
    ```
    To compute total volume, `findAll()` loads **every single transaction in the database** into JVM memory and streams it! In production with 100,000+ orders, this will trigger severe GC pauses and an `OutOfMemoryError`. It must be replaced by a single JPQL/SQL aggregate: `SELECT COALESCE(SUM(t.quantity * t.price), 0) FROM Transaction t`.
* **In-Memory User Pagination & Filtering:**  
  * **Evidence:** [AdminUserController.java#L58-L88](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/admin/AdminUserController.java#L58-L88)  
  * **Finding:** `userRepository.findAll()` pulls all users into heap memory and manually performs Java stream filtering and `.subList(start, end)` pagination instead of using Spring Data JPA `Pageable` and database-level `WHERE` clauses.
* **Business Logic inside Controllers:**  
  * **Evidence:** [AdminUserController.java#L107-L145](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/admin/AdminUserController.java#L107-L145)  
  * **Finding:** Status transition logic, reason assignment, email notifications, and audit logging are implemented directly in `AdminUserController` rather than a dedicated `UserService`.

### 4.4 N+1 Queries & Missing Database Indexes
* **N+1 Entity Fetching:**  
  * **Evidence:** [Portfolio.java#L13-L17](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/model/Portfolio.java#L13-L17) and [Transaction.java#L16-L20](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/model/Transaction.java#L16-L20)  
  * **Finding:** Both entities specify `@ManyToOne(fetch = FetchType.EAGER)` on `User` and `Stock`. Loading a list of transactions executes individual subsequent SQL queries for every row's user and stock. Should be `FetchType.LAZY` with `JOIN FETCH` queries when eager loading is required.
* **Missing Database Indexes:**  
  * **Evidence:** [Transaction.java#L9](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/model/Transaction.java#L9), [Portfolio.java#L6](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/model/Portfolio.java#L6), [User.java#L12](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/model/User.java#L12)  
  * **Finding:**
    * `transactions`: No indexes on `user_id`, `stock_id`, `date`, or `status`. Lookups like `findByUserOrderByDateDesc` perform full table scans.
    * `portfolio`: No unique index on `(user_id, stock_id)`. Nothing stops duplicate holding rows from being inserted under concurrent execution.
    * `users`: No indexes on `role`, `access_status`, `kyc_status`, or `account_status`.

### 4.5 Pagination on List Endpoints
* **Evidence:**
  * [TradeController.java#L55-L59](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/TradeController.java#L55-L59): `GET /trade/history` returns unbounded `List<TransactionDTO>`.
  * [PortfolioController.java#L46-L51](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/PortfolioController.java#L46-L51): `GET /portfolio/holdings` returns unbounded list.
  * [StockController.java#L23-L31](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/StockController.java#L23-L31): `GET /stocks`, `GET /stocks/live` return unbounded lists.
  * [StockController.java#L61-L64](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/StockController.java#L61-L64): `GET /stocks/search` returns unbounded list.

---

## 5. Database and Persistence

**Score: 3.0 / 10** | **Verdict: Not Production-Ready**

### 5.1 In-Memory H2 Database
* **Evidence:** [pom.xml#L45-L49](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/pom.xml#L45-L49) and [application.properties#L3-L6](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/resources/application.properties#L3-L6)  
* **Finding:** The application runs on `jdbc:h2:mem:equitrade`. Any application crash, deployment, or server restart destroys all trading ledgers, user accounts, and financial records. There is no production database driver (e.g., PostgreSQL or MySQL) in `pom.xml`.

### 5.2 Schema Migrations (Flyway / Liquibase)
* **Evidence:** [pom.xml](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/pom.xml) and [application.properties#L8](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/resources/application.properties#L8)  
* **Finding:** `spring.jpa.hibernate.ddl-auto=update` is used. Neither Flyway nor Liquibase is included. In production, Hibernate auto-update is hazardous: it cannot reliably handle column renames, drops, index creation, data migrations, or rollbacks.

### 5.3 Data Loss / State Reset Hazard in `DataInitializer`
* **Evidence:** [DataInitializer.java#L120-L140](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/config/DataInitializer.java#L120-L140)  
* **Finding:** Even if switched to a persistent database, `DataInitializer.java` executes on every boot via `CommandLineRunner`. In `createOrUpdateUser`:
  ```java
  User user = userRepository.findByEmail(email).orElse(null);
  ...
  user.setPassword(passwordEncoder.encode(password)); // <-- Overwrites password!
  user.setBalance(balance);                           // <-- Overwrites balance!
  userRepository.save(user);
  ```
  Every reboot resets existing users' passwords back to defaults and wipes their live cash balance back to initial seed values (e.g. ₹250,000 for `trader@equitrade.com`).

---

## 6. Automated Testing & Verification

**Score: 1.0 / 10** | **Verdict: Total Absence of Test Coverage**

### 6.1 Test Coverage Analysis
* **What is Covered:**
  * Exactly 1 test exists: [DemoApplicationTests.java#L9-L11](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/test/java/com/example/demo/DemoApplicationTests.java#L9-L11), which merely boots the Spring ApplicationContext.
* **What is Completely Missing:**
  * **Unit Tests:** Zero unit tests for `TradeService` (buy, sell, partial sell, insufficient funds, insufficient shares, trade immutability).
  * **Financial Math Tests:** Zero tests verifying fee calculations, GST, brokerage tariffs, and rounding accuracy.
  * **Auth & Security Tests:** Zero tests verifying JWT issuance, validation, password encryption, rate limiting, and access control.
  * **Concurrency Tests:** Zero multi-threaded integration tests verifying race condition protection against double-spends.
  * **Controller Tests:** Zero `MockMvc` or `WebMvcTest` tests verifying status codes, `@Valid` validation errors, and JSON response contracts.

### 6.2 Execution Results (`mvn clean test`)
The test suite was executed via Maven in `c:\fullstack\EquiTrade\EquiTrade1\demo`. Below is the verified build output:

```text
[INFO] Scanning for projects...
[INFO] --------------------------< com.example:demo >--------------------------
[INFO] Building EquiTrade 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] --- clean:3.4.1:clean (default-clean) @ demo ---
[INFO] Deleting C:\fullstack\EquiTrade\EquiTrade1\demo\target
[INFO] --- resources:3.3.1:resources (default-resources) @ demo ---
[INFO] --- compiler:3.14.0:compile (default-compile) @ demo ---
[INFO] Compiling 110 source files with javac [debug parameters release 25] to target\classes
[INFO] --- resources:3.3.1:testResources (default-testResources) @ demo ---
[INFO] --- compiler:3.14.0:testCompile (default-testCompile) @ demo ---
[INFO] Compiling 1 source file with javac [debug parameters release 25] to target\test-classes
[INFO] --- surefire:3.5.3:test (default-test) @ demo ---
[INFO] Running com.example.demo.DemoApplicationTests
2026-10-02T14:07:06.822+05:30  INFO 8544 --- [main] c.e.demo.DemoApplicationTests : Starting DemoApplicationTests using Java 25.0.3...
2026-10-02T14:07:11.176+05:30  INFO 8544 --- [main] c.e.demo.DemoApplicationTests : Started DemoApplicationTests in 4.515 seconds
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 6.282 s -- in com.example.demo.DemoApplicationTests
[INFO] 
[INFO] Results:
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  10.048 s
[INFO] ------------------------------------------------------------------------
```
* **Verdict:** Build succeeded only because there are no assertions testing business logic.

---

## 7. Operations and Observability

**Score: 3.5 / 10** | **Verdict: Inadequate Telemetry for Production**

### 7.1 Health & Metrics Endpoints (Actuator)
* **Evidence:** [pom.xml](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/pom.xml)  
* **Finding:** `spring-boot-starter-actuator` is **not included**. There are no `/actuator/health`, `/actuator/info`, `/actuator/metrics`, or Kubernetes liveness/readiness probes. Load balancers and container orchestrators cannot verify service health.

### 7.2 Structured Logging & Tracing
* **Evidence:** [application.properties#L36](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/resources/application.properties#L36) and [AuthTokenFilter.java#L75](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/AuthTokenFilter.java#L75)  
* **Finding:**
  1. Default standard-out logging only. No structured JSON log appender (Logstash/JSON) for ingestion by ELK, Datadog, or Grafana Loki.
  2. No correlation ID / Trace ID (MDC) attached to HTTP requests to trace user orders across threads and services.
  3. No audit log masking of PII (PAN, phone numbers).

### 7.3 Configuration & Environment Profiles
* **Evidence:** [application.properties](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/resources/application.properties)  
* **Finding:**
  1. Only one flat `application.properties` file exists; no `application-dev.properties` or `application-prod.properties`.
  2. H2 console is permanently enabled (`spring.h2.console.enabled=true`).
  3. Default database and mail credentials fallback to hardcoded strings rather than requiring environment variables in production profiles.

---

## Final Overall Score & Evaluation Matrix

$$\text{Overall Score} = \frac{2.5 + 4.0 + 4.5 + 4.0 + 3.0 + 1.0 + 3.5}{7} = \mathbf{3.2\ /\ 10}$$

### Deployment Recommendation: **REJECTED FOR PRODUCTION**
The backend contains multiple critical security vulnerabilities (authentication bypass, IDOR, unauthenticated stock injection), catastrophic race conditions (double-spends on balance and overselling), floating-point monetary calculations, in-memory table-wide scanning, and zero regression test coverage.

---

## Top 10 Prioritized Fix List

| Rank | Issue & Risk | File & Line | Business Impact |
|:---:|---|---|---|
| **1** | Remove Authentication Bypass Backdoor (`Token-` prefix) | [JwtUtils.java#L68-L70](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/JwtUtils.java#L68-L70) | Complete system takeover by any unauthenticated attacker |
| **2** | Fix Security Filter: Remove `.anyRequest().permitAll()`, lock down User and Stock endpoints | [SecurityConfig.java#L53-L60](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/SecurityConfig.java#L53-L60) | Prevents unauthenticated stock creation and unauthorized API access |
| **3** | Eliminate IDOR & Anonymous Fallback in `SecurityUtil` and Trading Controllers | [SecurityUtil.java#L31-L46](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/security/SecurityUtil.java#L31-L46), [TradeController.java#L31](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/TradeController.java#L31) | Stops attackers from trading with or viewing other users' accounts |
| **4** | Prevent Double-Click & Race Conditions with Optimistic Locking (`@Version`) & Row Locking | [User.java#L30](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/model/User.java#L30), [Portfolio.java#L19](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/model/Portfolio.java#L19) | Prevents balance double-spending and stock overselling |
| **5** | Convert Money & Price Fields from `double` to `BigDecimal` | [User.java](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/model/User.java), [Portfolio.java](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/model/Portfolio.java), [TradeService.java](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java) | Eliminates IEEE 754 financial calculation rounding drift |
| **6** | Replace In-Memory Table Scans (`findAll().stream()`) with Database Aggregations & Pagination | [AdminController.java#L54-L56](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/AdminController.java#L54-L56), [AdminUserController.java#L58](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/admin/AdminUserController.java#L58) | Prevents JVM OutOfMemoryError and crashes under production load |
| **7** | Enforce `@Valid` on All Request Bodies & Add Domain Exception Handling | [TradeController.java](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/TradeController.java), [AuthController.java](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/Controller/AuthController.java), [GlobalExceptionHandler.java](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/exception/GlobalExceptionHandler.java) | Rejects malformed requests and stops internal error leakage |
| **8** | Prevent State & Password Overwrite on Startup in `DataInitializer` | [DataInitializer.java#L123-L139](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/java/com/example/demo/config/DataInitializer.java#L123-L139) | Stops production balance resets and password overwrites |
| **9** | Replace Field Injection (`@Autowired`) with Constructor Injection | Across all Controllers and Services | Ensures immutability, testability, and clean dependency management |
| **10** | Add Spring Boot Actuator & PostgreSQL Production Configuration | [pom.xml](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/pom.xml), [application.properties](file:///c:/fullstack/EquiTrade/EquiTrade1/demo/src/main/resources/application.properties) | Enables production database persistence and container health checks |

---

## Exact Code Changes for Top 10 Fixes

### Fix 1: Eliminate Authentication Bypass Backdoor

**Target File:** `EquiTrade1/demo/src/main/java/com/example/demo/security/JwtUtils.java`

#### Before:
```java
// Lines 68-70
if (clean.startsWith("Token-")) {
    return clean.substring(6).trim();
}

// Lines 89-91
if (clean.startsWith("Token-")) {
    return !clean.substring(6).trim().isEmpty();
}
```

#### After:
```java
// REMOVE all mock Token- prefixes completely.
// All tokens MUST be cryptographically verified HMAC-SHA256 JWTs.
public String getEmailFromToken(String token) {
    if (token == null) return null;
    String clean = token.trim();
    if (clean.startsWith("Bearer ")) {
        clean = clean.substring(7).trim();
    }
    try {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(clean)
                .getPayload();
        return claims.getSubject();
    } catch (JwtException | IllegalArgumentException e) {
        return null;
    }
}

public boolean validateToken(String token) {
    if (token == null || token.isBlank()) return false;
    String clean = token.trim();
    if (clean.startsWith("Bearer ")) {
        clean = clean.substring(7).trim();
    }
    try {
        Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(clean);
        return true;
    } catch (JwtException | IllegalArgumentException e) {
        return false;
    }
}
```

---

### Fix 2: Secure Filter Chain, Disallow Unauthenticated Stock Injections, and Fix CORS

**Target File:** `EquiTrade1/demo/src/main/java/com/example/demo/security/SecurityConfig.java`

#### Before:
```java
// Lines 40-60
.headers(headers -> headers.frameOptions(frame -> frame.disable()))
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    .requestMatchers(
        "/",
        "/*.html",
        "/admin/*.html",
        "/css/**",
        "/js/**",
        "/assets/**",
        "/h2-console/**",
        "/auth/**",
        "/api/auth/**",
        "/stocks/**",
        "/api/stocks/**"
    ).permitAll()
    .requestMatchers("/admin/**", "/api/admin/**").hasAnyRole(
        "SUPER_ADMIN", "ADMIN", "KYC_OFFICER", "SUPPORT", "RISK_OFFICER", "FINANCE", "COMPLIANCE_OFFICER"
    )
    .anyRequest().permitAll()
);
```

#### After:
```java
.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    // Static assets & public pages
    .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/assets/**").permitAll()
    // Auth endpoints
    .requestMatchers("/auth/**", "/api/auth/**").permitAll()
    // Read-only stock catalog endpoints
    .requestMatchers(HttpMethod.GET, "/stocks/**", "/api/stocks/**").permitAll()
    // Restrict stock modifications to Admin roles
    .requestMatchers(HttpMethod.POST, "/stocks/**", "/api/stocks/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "RISK_OFFICER")
    .requestMatchers(HttpMethod.PUT, "/stocks/**", "/api/stocks/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "RISK_OFFICER")
    .requestMatchers(HttpMethod.DELETE, "/stocks/**", "/api/stocks/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
    // Admin backend endpoints & UI pages
    .requestMatchers("/admin/*.html", "/admin/**", "/api/admin/**").hasAnyRole(
        "SUPER_ADMIN", "ADMIN", "KYC_OFFICER", "SUPPORT", "RISK_OFFICER", "FINANCE", "COMPLIANCE_OFFICER"
    )
    // Trading, portfolio, and watchlist REQUIRE authentication
    .requestMatchers("/trade/**", "/api/trade/**", "/portfolio/**", "/api/portfolio/**", "/watchlist/**", "/api/watchlist/**").authenticated()
    .anyRequest().authenticated()
);
```

And in CORS configuration:
```java
// Disallow wildcard pattern when credentials are enabled
@Value("${app.cors.allowed-origins:http://localhost:8081,http://localhost:3000}")
private List<String> allowedOrigins;

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
    configuration.setExposedHeaders(List.of("Authorization"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

---

### Fix 3: Eliminate IDOR and Unsafe Fallbacks in Security & Controllers

**Target File 1:** `EquiTrade1/demo/src/main/java/com/example/demo/security/SecurityUtil.java`

#### Before:
```java
public User resolveUser(Long requestedUserId) {
    if (requestedUserId != null) {
        Optional<User> userOpt = userRepository.findById(requestedUserId);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }
    }
    User current = getCurrentUser();
    if (current != null) {
        return current;
    }
    return userRepository.findAll().stream()
        .filter(u -> !"ROLE_ADMIN".equals(u.getRole()))
        .findFirst()
        .orElse(userRepository.findAll().stream().findFirst().orElse(null));
}
```

#### After:
```java
public User getRequiredCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
        throw new org.springframework.security.access.AccessDeniedException("Full authentication is required to access this resource");
    }
    String email = auth.getName();
    return userRepository.findByEmail(email)
            .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Authenticated user record not found"));
}

public User resolveUser(Long requestedUserId) {
    User current = getRequiredCurrentUser();
    // Non-admin users can ONLY act on their own account
    if (requestedUserId != null && !requestedUserId.equals(current.getId())) {
        boolean isAdmin = current.getRole().contains("ADMIN");
        if (!isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to perform actions for another user.");
        }
        return userRepository.findById(requestedUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + requestedUserId));
    }
    return current;
}
```

**Target File 2:** `EquiTrade1/demo/src/main/java/com/example/demo/Controller/PortfolioController.java`

#### Before:
```java
@GetMapping("/{userId}")
public ResponseEntity<?> getPortfolio(@PathVariable Long userId) {
    if (userId == null || userId <= 0) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, "Invalid user ID"));
    }
    User user = userRepository.findById(userId).orElse(null);
    if (user == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, "User not found"));
    }
    return ResponseEntity.ok(portfolioService.getPortfolio(user));
}
```

#### After:
```java
@GetMapping("/{userId}")
public ResponseEntity<?> getPortfolio(@PathVariable Long userId) {
    User currentUser = securityUtil.getRequiredCurrentUser();
    // IDOR Protection: only owner or admins may view
    if (!currentUser.getId().equals(userId) && !currentUser.getRole().contains("ADMIN")) {
        throw new org.springframework.security.access.AccessDeniedException("Access denied: You cannot view another user's portfolio");
    }
    User targetUser = currentUser.getId().equals(userId) ? currentUser :
            userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    return ResponseEntity.ok(portfolioService.getPortfolioSummary(targetUser));
}
```

---

### Fix 4: Prevent Race Conditions via Optimistic Locking (`@Version`) & Row Locking

**Target File 1:** `EquiTrade1/demo/src/main/java/com/example/demo/model/User.java` and `Portfolio.java`

#### Add to `User.java`:
```java
@Version
private Long version;

public Long getVersion() {
    return version;
}
```

#### Add to `Portfolio.java`:
```java
@Version
private Long version;

public Long getVersion() {
    return version;
}
```

**Target File 2:** `EquiTrade1/demo/src/main/java/com/example/demo/repository/UserRepository.java`

```java
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT u FROM User u WHERE u.id = :id")
Optional<User> findByIdForUpdate(@Param("id") Long id);
```

**Target File 3:** `EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java`

#### Acquire pessimistic lock on user and portfolio during order execution:
```java
// Inside buyStock():
User user = userRepository.findByIdForUpdate(resolvedUser.getId())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

// Inside sellStock():
User user = userRepository.findByIdForUpdate(resolvedUser.getId())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
Optional<Portfolio> holdingOpt = portfolioRepository.findByUserAndStockForUpdate(user, stock);
```

---

### Fix 5: Convert Financial Currency Fields from `double` to `BigDecimal`

**Target File 1:** `EquiTrade1/demo/src/main/java/com/example/demo/model/User.java`

#### Before:
```java
private double balance = 100000.0;
private double blockedAmount = 0.0;
```

#### After:
```java
@Column(precision = 19, scale = 4, nullable = false)
private BigDecimal balance = new BigDecimal("100000.0000");

@Column(precision = 19, scale = 4, nullable = false)
private BigDecimal blockedAmount = BigDecimal.ZERO;
```

**Target File 2:** `EquiTrade1/demo/src/main/java/com/example/demo/model/Stock.java`

#### Before:
```java
private double price;
```

#### After:
```java
@Column(precision = 19, scale = 4, nullable = false)
private BigDecimal price = BigDecimal.ZERO;
```

**Target File 3:** `EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java`

#### Calculation Refactoring:
```java
BigDecimal unitPrice = stock.getPrice();
BigDecimal quantity = BigDecimal.valueOf(request.getQuantity());
BigDecimal grossPrice = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);

FeeCalculationService.FeeBreakdown fees = feeCalculationService.calculateFees(grossPrice, "BUY");
BigDecimal totalRequired = grossPrice.add(fees.getTotalFees());

if (user.getBalance().compareTo(totalRequired) < 0) {
    throw new InsufficientFundsException("Insufficient Balance. Required: ₹" + totalRequired + ", Available: ₹" + user.getBalance());
}

user.setBalance(user.getBalance().subtract(totalRequired));
userRepository.save(user);
```

---

### Fix 6: Replace In-Memory Full Table Scans with Database Aggregations

**Target File 1:** `EquiTrade1/demo/src/main/java/com/example/demo/repository/TransactionRepository.java`

#### Add Database Aggregation Query:
```java
@Query("SELECT COALESCE(SUM(t.quantity * t.price), 0.0) FROM Transaction t")
Double calculateTotalTradingVolume();
```

**Target File 2:** `EquiTrade1/demo/src/main/java/com/example/demo/Controller/AdminController.java`

#### Before:
```java
// Lines 54-56: Pulls entire table into heap!
double totalVolume = transactionRepository.findAll().stream()
        .mapToDouble(t -> t.getQuantity() * t.getPrice())
        .sum();
```

#### After:
```java
double totalVolume = transactionRepository.calculateTotalTradingVolume();
```

**Target File 3:** `EquiTrade1/demo/src/main/java/com/example/demo/Controller/admin/AdminUserController.java`

#### Replace `userRepository.findAll()` in-memory filtering with database-level `Pageable` query:
```java
// Delegate to Spring Data JPA Specification or Pageable query:
Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
Page<User> userPage = userRepository.findFilteredUsers(query, role, accessStatus, kycStatus, accountStatus, pageable);
```

---

### Fix 7: Enforce `@Valid` on Controllers & Implement Typed Domain Exceptions

**Target File 1:** `EquiTrade1/demo/src/main/java/com/example/demo/dto/TradeRequest.java`

```java
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class TradeRequest {
    @NotNull(message = "Stock ID is required")
    @Min(value = 1, message = "Valid Stock ID must be greater than 0")
    private Long stockId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1 share")
    private Integer quantity;
    
    // ...
}
```

**Target File 2:** `EquiTrade1/demo/src/main/java/com/example/demo/Controller/TradeController.java`

#### Before:
```java
@PostMapping("/buy")
public ResponseEntity<ApiResponse> buyStock(@RequestBody TradeRequest request) { ... }
```

#### After:
```java
@PostMapping("/buy")
public ResponseEntity<ApiResponse> buyStock(@Valid @RequestBody TradeRequest request) {
    TradeResultDTO result = tradeService.buyStock(request);
    return ResponseEntity.ok(new ApiResponse(true, "Stock Purchased Successfully", result.getNewBalance()));
}
```

**Target File 3:** `EquiTrade1/demo/src/main/java/com/example/demo/exception/GlobalExceptionHandler.java`

#### Do not expose raw internal messages on 500 errors:
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
    logger.error("Unhandled internal server exception: ", ex);
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    body.put("error", "Internal Server Error");
    body.put("message", "An unexpected error occurred. Please contact support with incident reference ID.");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
}

@ExceptionHandler(InsufficientFundsException.class)
public ResponseEntity<ApiResponse> handleInsufficientFunds(InsufficientFundsException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, ex.getMessage()));
}
```

---

### Fix 8: Prevent State Overwrite & Password Reset in `DataInitializer`

**Target File:** `EquiTrade1/demo/src/main/java/com/example/demo/config/DataInitializer.java`

#### Before:
```java
// Lines 123-139
User user = userRepository.findByEmail(email).orElse(null);
if (user == null) {
    user = new User();
    user.setEmail(email);
}
user.setUsername(username);
user.setPassword(passwordEncoder.encode(password)); // Overwrites live passwords!
user.setBalance(balance);                           // Overwrites live balances!
return userRepository.save(user);
```

#### After:
```java
// ONLY seed users if they do not exist! Never mutate existing accounts on reboot.
Optional<User> existingOpt = userRepository.findByEmail(email);
if (existingOpt.isPresent()) {
    return existingOpt.get(); // Preserve live balance, live password, and KYC state!
}

User user = new User();
user.setEmail(email);
user.setUsername(username);
user.setPassword(passwordEncoder.encode(password));
user.setRole(role);
user.setAccessStatus(accessStatus);
user.setKycStatus(kycStatus);
user.setAccountStatus(accountStatus);
user.setDeclineReason(declineReason);
user.setBalance(balance);
user.setPhone(phone);
user.setCreatedAt(LocalDateTime.now());
user.setUpdatedAt(LocalDateTime.now());
return userRepository.save(user);
```

---

### Fix 9: Refactor from Field Injection (`@Autowired`) to Constructor Injection

**Target File:** `EquiTrade1/demo/src/main/java/com/example/demo/service/TradeService.java`

#### Before:
```java
@Service
public class TradeService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private PortfolioRepository portfolioRepository;
    ...
}
```

#### After:
```java
@Service
public class TradeService {

    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final RiskAlertRepository riskAlertRepository;
    private final FeeCalculationService feeCalculationService;
    private final AuditLogService auditLogService;
    private final SecurityUtil securityUtil;

    public TradeService(
            UserRepository userRepository,
            StockRepository stockRepository,
            PortfolioRepository portfolioRepository,
            TransactionRepository transactionRepository,
            WalletTransactionRepository walletTransactionRepository,
            RiskAlertRepository riskAlertRepository,
            FeeCalculationService feeCalculationService,
            AuditLogService auditLogService,
            SecurityUtil securityUtil) {
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
        this.portfolioRepository = portfolioRepository;
        this.transactionRepository = transactionRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.riskAlertRepository = riskAlertRepository;
        this.feeCalculationService = feeCalculationService;
        this.auditLogService = auditLogService;
        this.securityUtil = securityUtil;
    }
    // ...
}
```

---

### Fix 10: Add Spring Boot Actuator & PostgreSQL Production Configuration

**Target File 1:** `EquiTrade1/demo/pom.xml`

```xml
<!-- Spring Boot Actuator for Production Health & Metrics -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- PostgreSQL Driver for Production Persistence -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Flyway for Database Versioning & Migrations -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

**Target File 2:** `EquiTrade1/demo/src/main/resources/application.properties`

```properties
# Actuator Configuration
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
management.endpoint.health.probes.enabled=true

# Strict JWT Secret Requirement (Must be provided via environment variable in production)
jwt.secret=${JWT_SECRET}
jwt.expiration.ms=${JWT_EXPIRATION_MS:3600000}

# Production PostgreSQL Profile Template (activate via SPRING_PROFILES_ACTIVE=prod)
# spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:equitrade}
# spring.datasource.username=${DB_USER:postgres}
# spring.datasource.password=${DB_PASSWORD}
# spring.jpa.hibernate.ddl-auto=validate
# spring.flyway.enabled=true
```

---

## Conclusion

The EquiTrade backend has strong functional roots, rich domain models, and complete feature coverage for an equity trading simulation platform. However, it cannot be considered production-ready until these **top 10 prioritized fixes** are implemented. Resolving the authentication backdoor, locking down endpoint permissions, preventing concurrency double-spends with database locking, using `BigDecimal` for currency arithmetic, and stopping startup balance resets will elevate EquiTrade from a vulnerable demo to an enterprise-grade, highly resilient trading engine.
