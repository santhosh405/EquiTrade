# EquiTrade · Advanced Equity Trading Platform

A modern, high-performance equity trading and portfolio management simulator built with **Spring Boot 3** and modern **Vanilla HTML5/CSS3/JavaScript**.

---

## 🌟 Overview & Key Features

- 🖥️ **Multi-Page Web Architecture**: Split into dedicated pages with real hyperlinks and shared components (navbar, footer, theme switcher, global search, and authentication guard).
- 📈 **Simulated Live Market Floor**: Background `@Scheduled` tasks fluctuate prices every 4 seconds with green/red cell flash animations.
- 📊 **Interactive Chart.js Visualizations**:
  - **Stock Detail**: Dynamic price trajectory with interactive **1D / 1W / 1M / 1Y** timeframe switches.
  - **Portfolio**: Asset allocation donut chart and cumulative Profit & Loss trend line.
- 💼 **Comprehensive Portfolio Desk**: Holdings tracking with weighted average buy prices, current valuation, and unrealized gains.
- 🛒 **Instant Order Execution**: Buy and sell orders with live order value calculation, purchasing power validation, and confirmation modals.
- ⭐ **Database-Backed Watchlist**: Star favorite stocks directly into the database with one-click toggles and market filtering.
- 🔍 **Global Autocomplete Search**: Search securities across symbols and company names from anywhere in the top navigation bar.
- 📥 **Audit Trail & CSV Export**: Complete trade logs with activity filters and one-click browser CSV export.
- 🔐 **JWT Authentication & Role Protection**:
  - Standard user trading desks.
  - Protected **Admin Dashboard** (`/admin.html`) with market telemetry, inventory catalog, and stock listing controls.
- 🎨 **Fintech Design System**:
  - Dark mode by default with seamless Light/Dark toggle persisted in `localStorage`.
  - Glassmorphism panels, soft gradients, Inter typography, and responsive layouts.

---

## 🗺️ Page Map (`demo/front end`)

| Page | Description | Access |
|---|---|---|
| [`index.html`](file:///c:/fullstack/EquiTrade/demo/front%20end/index.html) | Landing page with live market stat counters, top 4 gainers/losers preview cards, feature highlights, and navigation links. | Public |
| [`market.html`](file:///c:/fullstack/EquiTrade/demo/front%20end/market.html) | Full stock market board with search, sector dropdown, movement filter (All / Gainers / Losers / Watchlist), price range filter, sorting, pagination, and live price flashes. | Public |
| [`stock.html`](file:///c:/fullstack/EquiTrade/demo/front%20end/stock.html)`?symbol=XXX` | In-depth security analysis page with Chart.js price line chart (1D/1W/1M/1Y), key statistics (range, prev close, sentiment), and quick Buy/Sell links. | Public |
| [`trade.html`](file:///c:/fullstack/EquiTrade/demo/front%20end/trade.html) | Buy and sell trading desk with live order calculation, available balance checks, and confirmation modal. | Authenticated |
| [`portfolio.html`](file:///c:/fullstack/EquiTrade/demo/front%20end/portfolio.html) | Holdings table, summary cards (Total Invested, Current Value, Day P&L, P&L %), asset allocation donut chart, and cumulative P&L line chart. | Authenticated |
| [`history.html`](file:///c:/fullstack/EquiTrade/demo/front%20end/history.html) | Historical transactions log with type filtering (All / BUY / SELL), symbol search, and CSV export. | Authenticated |
| [`admin.html`](file:///c:/fullstack/EquiTrade/demo/front%20end/admin.html) | Admin control center: total users, trades, turnover telemetry, most traded stocks, add/edit listing form, and delete with confirmation modal. | Admin Only |
| [`login.html`](file:///c:/fullstack/EquiTrade/demo/front%20end/login.html) | Login authentication with one-click demo credentials and JWT session storage. | Public |
| [`register.html`](file:///c:/fullstack/EquiTrade/demo/front%20end/register.html) | Registration form with validation and ₹1,00,000 initial virtual cash balance. | Public |
| [`404.html`](file:///c:/fullstack/EquiTrade/demo/front%20end/404.html) | Friendly error page with quick navigation back to home or market. | Public |

### Shared Modules
- `css/styles.css`: Centralized CSS variables, glassmorphism, responsive grid, animations (`@keyframes flash-green`, `@keyframes flash-red`), skeleton loaders.
- `js/api.js`: Unified API client with automatic port detection, JWT headers, error handling, 401/403 redirection, INR currency formatters (`₹`), and SVG sparklines.
- `js/auth.js`: Session management, role checks (`isLoggedIn`, `isAdmin`), and route guards.
- `js/ui.js`: Toast notification stack, modals, animated number counters, and loaders.
- `js/components.js`: Injects identical navbar, theme switch, global search autocomplete, and footer across all pages.

---

## 🔑 Demo Credentials

| Role | Email | Password | Initial Balance | Permissions |
|---|---|---|---|---|
| **Administrator** | `admin@equitrade.com` | `admin123` | ₹5,00,000 | Full access + Admin Console |
| **Trader** | `trader@equitrade.com` | `trader123` | ₹2,50,000 | Pre-loaded portfolio & trade desk |
| **Demo User** | `demo@equitrade.com` | `demo123` | ₹1,00,000 | Fresh trading account |

*(Tip: The `login.html` page includes one-click buttons to instantly fill these credentials).*

---

## 📡 Backend REST API Endpoints

All endpoints support both `/api/...` and legacy prefixes (`/stocks`, `/trade`, etc.) for seamless compatibility.

### Authentication (`/auth` & `/api/auth`)
- `POST /api/auth/register` — Register a new account (`username`, `email`, `password`)
- `POST /api/auth/login` — Authenticate and receive JWT token + user details
- `GET /api/auth/me` — Retrieve profile & current balance for authenticated user

### Stocks (`/stocks` & `/api/stocks`)
- `GET /api/stocks` — List all active stocks
- `GET /api/stocks/live` — Real-time price snapshot (polled for green/red flash updates)
- `GET /api/stocks/paged` — Filtered & paginated stocks (`query`, `sector`, `minPrice`, `maxPrice`, `page`, `size`, `sortBy`, `sortDir`)
- `GET /api/stocks/sectors` — List distinct industry sectors
- `GET /api/stocks/gainers` — Top 4 gainers by percentage change
- `GET /api/stocks/losers` — Top 4 losers by percentage change
- `GET /api/stocks/search?q=...` — Search by ticker symbol or company name
- `GET /api/stocks/symbol/{symbol}` — Get single stock by symbol
- `GET /api/stocks/{id}` — Get single stock by ID
- `GET /api/stocks/{symbol}/history?range={1D|1W|1M|1Y}` — Historical price series
- `POST /api/stocks/add` — Add a new stock listing

### Watchlist (`/watchlist` & `/api/watchlist`)
- `GET /api/watchlist` — Get user's saved watchlist
- `POST /api/watchlist/{stockId}` — Add stock to watchlist
- `DELETE /api/watchlist/{stockId}` — Remove stock from watchlist

### Trade Desk (`/trade` & `/api/trade`)
- `POST /api/trade/buy` — Execute a BUY order (`stockId`, `quantity`)
- `POST /api/trade/sell` — Execute a SELL order (`stockId`, `quantity`)
- `GET /api/trade/history` — Get transaction log for authenticated user

### Portfolio (`/portfolio` & `/api/portfolio`)
- `GET /api/portfolio` — Portfolio summary (`totalInvested`, `totalCurrentValue`, `totalProfitLoss`, `totalProfitLossPercent`, `holdings`)
- `GET /api/portfolio/{userId}` — Legacy holdings list by user ID

### Admin (`/admin` & `/api/admin`)
- `GET /api/admin/dashboard` — Platform telemetry (`totalUsers`, `totalTrades`, `totalStocks`, `totalTradeVolume`, `mostTradedStocks`)
- `GET /api/admin/stocks` — Inventory of all listed stocks
- `POST /api/admin/stocks` — Add new stock
- `PUT /api/admin/stocks/{id}` — Update existing stock
- `DELETE /api/admin/stocks/{id}` — Delete stock listing and purge cascades

---

## 🚀 Running the Project

### 1. Start the Backend Server (Spring Boot)

Open a terminal in the project directory:

```bash
cd demo
mvn clean spring-boot:run
```

The Spring Boot backend will start on **http://localhost:8081** and automatically seed:
- 3 demo users (`admin`, `trader`, `demo_user`)
- 20 diverse stocks across 9 sectors
- Price history data
- Initial portfolio holdings and trade history
- Background price simulation running every 4 seconds

### 2. Open the Frontend

You can open the frontend in either of two ways:

#### Option A: Built-in Spring Boot Web Server
Navigate directly to:
**http://localhost:8081/index.html**

#### Option B: VS Code Live Server / Local HTTP Server
Run Live Server on the `demo/front end` directory (e.g. `http://127.0.0.1:5500/demo/front%20end/index.html` or `http://localhost:5500`).
The frontend automatically connects to `http://localhost:8081` via CORS!

---

## 🛠️ Verification & Build Status

- **Build Tool**: Apache Maven
- **Java Version**: 25 (with Spring Boot 3.5.5)
- **Unit & Integration Tests**: 100% Passing (`mvn clean install` verified)
- **Database**: H2 in-memory (`jdbc:h2:mem:equitrade`)
