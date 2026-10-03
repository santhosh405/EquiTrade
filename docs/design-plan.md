# EquiTrade Design Plan & Architecture Specification
**Direction:** *"A calm trading terminal where color means money"*

---

## 1. Design Principles

1. **Color Means Money**
   Color is strictly reserved for financial meaning: green for gains, red for losses, and single-hue Action Blue (`#2150D6`) for interactive triggers. Surface backgrounds, borders, typography, and card containers remain neutral greys and whites. Color is never used as background gradient decoration.

2. **Dense Clarity Over Decorative Fluff**
   Real traders need fast visual scans of numbers. The design emphasizes tabular tables, right-aligned numerical data, monospace/tabular numerals (`font-variant-numeric: tabular-nums`), clean borders, and clear summary strips rather than endless cards with rounded corners.

3. **In-Context Action via Order Drawer**
   Traders should never lose their place. Clicking "Buy" or "Sell" anywhere in the platform (Market table, Stock detail, Portfolio, Watchlist) opens a persistent right-side slide-over Order Drawer with live pricing, cost breakdown, cash validation, and instant confirmation—keeping the user anchored in their workflow.

4. **Honest, Unambiguous Communication**
   Indian market context is central: amounts are formatted in INR with standard Indian lakhs/crores comma separation (`en-IN`), practice virtual capital is clearly designated (₹5,00,000 practice funds), and exchange hours follow NSE live IST rules. Buttons state the exact operation: *"Buy 10 TCS"*, *"Place buy order"*, *"Remove from watchlist"*.

5. **Motion That Answers, Never Entertains**
   Motion is functional: a crisp slide-over drawer, a smooth crosshair tooltip on the candlestick canvas, and subtle 600ms price tick flashes (green up / red down) on table rows. No bounce effects, no hovering card lift, and full compliance with `prefers-reduced-motion`.

---

## 2. Design Tokens

### Color Palette (tokens.css)

| Token Name | Light Theme | Dark Theme | Purpose / Usage |
| :--- | :--- | :--- | :--- |
| `--color-ink` | `#111618` | `#EEF2F4` | Primary text, titles, prominent headers |
| `--color-slate` | `#5A656B` | `#8E9CA3` | Secondary text, meta captions, column headers |
| `--color-mist` | `#E5E8EB` | `#262E33` | Borders, table row dividers, subtle outlines |
| `--color-surface` | `#FFFFFF` | `#14191C` | Page base background and primary table fill |
| `--color-panel` | `#F7F9FA` | `#1B2226` | Subtle contrast containers, sidebars, drawer bases |
| `--color-gain` | `#0E9F6E` | `#2AC78A` | Positive P&L, gainers, upward ticks, advance indicators |
| `--color-gain-subtle` | `rgba(14, 159, 110, 0.10)` | `rgba(42, 199, 138, 0.14)` | Green badge fills, sparkline area gradients |
| `--color-loss` | `#D6393A` | `#F05252` | Negative P&L, losers, downward ticks, decline indicators |
| `--color-loss-subtle` | `rgba(214, 57, 58, 0.10)` | `rgba(240, 82, 82, 0.14)` | Red badge fills, sparkline area gradients |
| `--color-action` | `#2150D6` | `#3E72FF` | Action buttons, active navigation, focus outlines |
| `--color-action-hover`| `#1A3FB0` | `#2B5FE8` | Action button hover state |

### Typography

- **Family:** `Instrument Sans` (Google Fonts: weights 400, 500, 600, 700). Distinctive geometric sans with clean letterforms, distinct metrics, and robust tabular numeric glyphs.
- **Numbers:** `font-variant-numeric: tabular-nums` enforced globally on all numeric displays, currency values, percentages, quantities, and timestamps.
- **Scale:**
  - `Display`: 26px / 32px, 700 weight (Page title)
  - `Title`: 18px / 24px, 700 weight (Section title)
  - `Body`: 13px / 18px, 400 & 500 weight (Table cells, inputs, descriptions)
  - `Caption`: 11px / 15px, 500 & 600 weight (Table headers, meta labels, chips)
- **Line Length:** Max 72ch for reading copy.

### Radii & Shadow Hierarchy

- **Small (`3px`):** Table rows, input fields, select dropdowns, status chips, compact buttons.
- **Medium (`6px`):** Cards, content panels, chart wrappers, toolbar containers.
- **Large (`10px`):** Floating Order Drawer, confirmation modal dialogs.
- **Elevation:** Flat border-based structure on all page panels. Shadows applied only to floating overlays (`box-shadow: 0 12px 32px rgba(0,0,0,0.12)`).

---

## 3. Review Against Banned Defaults

| Banned Generic Fintech Default | Replaced With | Design Rationale |
| :--- | :--- | :--- |
| Purple/blue-violet gradients, glassmorphism, glow filters | Crisp flat borders (`--color-mist`) on clean neutral surfaces (`--color-surface` / `--color-panel`) | Replaces illegible decorative blur with calm, crisp contrast suitable for trading terminals. |
| Pitch-black dark mode with high-saturation neon | Neutral slate-tinted dark mode (`#14191C` base, `#1B2226` panel) with muted indicators | Eliminates eye strain and maintains readable contrast ratios exceeding WCAG AA. |
| All-caps tracked micro labels everywhere | Sentence case labels (`Day change`, `Prev close`, `Avg buy price`) in 11px Slate | Improves scanning speed; avoids visual screaming of unnecessary acronym-like labels. |
| Content chopped into rows of identical rounded cards | Dense data tables with sticky headers, subtle zebra borders, and row-hover quick actions | Real traders view watchlists as tables, not bulky cards that push data off-screen. |
| Appending `->` or `→` to every button | Direct verbs (*"Explore markets"*, *"Place order"*, *"Export CSV"*) | Cleans up repetitive noise and sets clear user expectations. |
| Hover lift (`translateY(-4px)`) and fade-in on every element | Stable static cards; subtle background highlight on table row hover only | Trading boards must not shift or wobble under the user's cursor while tracking ticks. |
| Vague marketing buzzwords (*"Trade smarter"*, *"state-of-the-art"*) | Accurate, plain descriptions (*"Practice Indian equity trading with ₹5,00,000 virtual cash on live NSE signals"*) | Builds trust and immediately establishes that EquiTrade is a simulation platform. |

---

## 4. Page Layouts & ASCII Wireframes

### Global Header, Live Index Tape & Mobile Navigation
```
+-----------------------------------------------------------------------------------------------+
| [Logo] EquiTrade   Markets  Portfolio  Orders  Watchlist  | [/] Search stocks... | [Bal: ₹5,00,000] [Theme] [User] |
+-----------------------------------------------------------------------------------------------+
| [LIVE] NIFTY 50: 24,840.10 (+0.42%) | SENSEX: 81,320.50 (+0.38%) | BANK NIFTY: 51,210.00 (-0.15%) | Top: TCS +2.4% |
+-----------------------------------------------------------------------------------------------+
```

### Slide-Over Order Drawer (Opens Anywhere on Buy/Sell click)
```
+-------------------------------------------------------------+------------------------------------+
|                                                             | [X] Order Ticket - TCS             |
|                                                             | Tata Consultancy Services Ltd      |
|                      CURRENT PAGE                           | ₹3,650.50 (+1.80%)                 |
|                   (Dimmed Backdrop)                         +------------------------------------+
|                                                             | [ BUY ]            |    SELL       |
|                                                             +------------------------------------+
|                                                             | Quantity (Shares)                  |
|                                                             | [ 10                             ] |
|                                                             +------------------------------------+
|                                                             | Order Value:             ₹36,505.00|
|                                                             | Est. Brokerage & Taxes:     ₹24.80 |
|                                                             | Total Consideration:     ₹36,529.80|
|                                                             | Available Practice Cash:₹5,00,000.00|
|                                                             +------------------------------------+
|                                                             | [ Place Buy Order for 10 Shares  ] |
+-------------------------------------------------------------+------------------------------------+
```

### 1. Home Page (Logged Out Hero & Live Market Pulse)
```
+-----------------------------------------------------------------------------------------------+
| PRACTICE TRADING SIMULATOR                                                                    |
| Master the Indian stock market with zero capital risk.                                        |
| Trade real-time simulated NSE equities with ₹5,00,000 practice cash.                         |
| [ Start Trading with ₹5,00,000 ]   [ Explore Markets ]                                        |
+-----------------------------------------------------------------------------------------------+
| MARKET SNAPSHOT                                                                               |
| [NIFTY 50 Overview Chart]              | [Top Gainers Table]        | [Top Pullbacks Table]   |
| 24,840.10 ▲ +104.20 (+0.42%)           | TCS        ₹3,650.50 +1.8% | WIPRO     ₹495.60 -1.2% |
| (TradingView Spark Area)               | RELIANCE   ₹2,750.25 +2.1% | INFY    ₹1,480.75 -0.6% |
+-----------------------------------------------------------------------------------------------+
```

### 2. Home Page (Logged In Dashboard)
```
+-----------------------------------------------------------------------------------------------+
| PORTFOLIO VALUE: ₹5,42,850.00 | DAY P&L: +₹3,240.50 (+0.60%) | UNREALIZED: +₹42,850.00 (+8.5%) | CASH: ₹4,57,150 |
+---------------------------------------------------------------+-------------------------------+
| PORTFOLIO PERFORMANCE vs NIFTY 50                             | QUICK WATCHLIST               |
| [1D] [1W] [1M] [3M] [1Y] [ALL]                                | Symbol   Price     Change     |
| (Lightweight Area Chart vs Nifty Benchmark Line)              | TCS      ₹3,650.50 ▲ +1.8% [Buy]|
|                                                               | INFY     ₹1,480.75 ▼ -0.6% [Buy]|
+---------------------------------------------------------------+-------------------------------+
| TOP MARKET MOVERS (Live NSE Feed)                             | RECENT ORDERS                 |
| TCS +1.8%  | RELIANCE +2.1% | TATAMOTORS +3.4% | ADANI +4.2%  | TCS  15 shares  EXECUTED ₹3,500|
+-----------------------------------------------------------------------------------------------+
```

### 3. Markets Page (Table-First Watchlist, Cards & Heatmap)
```
+-----------------------------------------------------------------------------------------------+
| Markets                                         [Search] [All Sectors v] [View: Table|Cards|Map]|
+-----------------------------------------------------------------------------------------------+
| Symbol & Company        | Last Price | Day Change | 30-Day Trend | Volume    | Sector | Action|
+-------------------------+------------+------------+--------------+-----------+--------+-------+
| TCS   Tata Consultancy  |  ₹3,650.50 |  ▲ +1.80%  | ~~~~~/\_     | 18.4L     | IT     | [B][S]|
| INFY  Infosys Ltd       |  ₹1,480.75 |  ▼ -0.65%  | \__/\        | 24.1L     | IT     | [B][S]|
| RELIANCE Reliance Ind   |  ₹2,750.25 |  ▲ +2.15%  | __/~~~\      | 32.8L     | Energy | [B][S]|
+-----------------------------------------------------------------------------------------------+
```

### 4. Stock Detail Page (2-Pane Split Chart & Order Station)
```
+---------------------------------------------------------------+-------------------------------+
| TCS · Tata Consultancy Services Ltd (NSE)                     | ORDER TICKET                  |
| ₹3,650.50  ▲ +₹64.50 (+1.80%)  Market Open                    | [ BUY ]     [ SELL ]          |
+---------------------------------------------------------------+ Qty: [ 10                   ] |
| [Candles|Line|Area]  [1D|1W|1M|3M|1Y|ALL]  [MA20] [MA50]      | Price:               ₹3,650.50|
| (TradingView Lightweight Candlestick Chart)                   | Total:              ₹36,505.00|
| (Volume Histogram Pane)                                       | Available Cash:   ₹4,57,150.00|
| (HUD: O: 3620.00 | H: 3660.00 | L: 3610.50 | C: 3650.50)      | [ Place Buy Order ]           |
+---------------------------------------------------------------+-------------------------------+
| KEY STATISTICS                                                | YOUR POSITION                 |
| Day Range: ₹3,610.50 - ₹3,660.00  | 52-Wk: ₹3,150 - ₹4,200    | Shares Owned:       15 shares |
| Prev Close: ₹3,586.00             | Sector: IT                | Current Value:      ₹54,757.50|
+---------------------------------------------------------------+-------------------------------+
```

### 5. Portfolio Page (Holdings Table, Donut, Comparative & P&L Bars)
```
+-----------------------------------------------------------------------------------------------+
| INVESTED: ₹3,50,000 | CURRENT: ₹3,84,500 | UNREALIZED P&L: +₹34,500 (+9.86%) | CASH: ₹1,57,500|
+---------------------------------------------------------------+-------------------------------+
| HOLDINGS (All columns visible, responsive)                    | ASSET ALLOCATION              |
| Symbol | Qty | Avg Price | Cur Price | Total Val | P&L (₹ & %) | (Donut Chart with Total Net   |
| TCS    |  15 |  ₹3,500.00|  ₹3,650.50|  ₹54,757.5| +2,257 (+4.3%)|  Worth in center + clickable   |
| REL    |  20 |  ₹2,680.00|  ₹2,750.25|  ₹55,005.0| +1,405 (+2.6%)|  legend with % labels)         |
+---------------------------------------------------------------+-------------------------------+
| PORTFOLIO PERFORMANCE vs NIFTY 50                             | P&L CONTRIBUTION PER HOLDING  |
| [1D] [1W] [1M] [3M] [1Y] [ALL]                                | TCS    [========+] +₹2,257.50 |
| (Comparative Area Curve vs NIFTYBEES Line)                    | REL    [=====+   ] +₹1,405.00 |
+---------------------------------------------------------------+-------------------------------+
```

---

## 5. Implementation Roadmap

1. **Tokens & Base CSS:** Create `css/tokens.css` with font imports, color tokens, layout resets, tabular figures, and dark theme definitions.
2. **Components Upgrade (`js/components.js`):**
   - Slim navigation topbar with live wallet balance, keyboard shortcut search with `/`, and user dropdown.
   - Global live NSE index tape (`NIFTY 50`, `SENSEX`, `BANK NIFTY`, top movers, market open/closed status badge).
   - Global slide-over Order Drawer component invoked by any Buy/Sell trigger across all pages.
   - Toast notification and modal engines.
3. **Core Utility & API (`js/api.js` & `js/ui.js`):**
   - Clean Indian currency formatter (`Intl.NumberFormat('en-IN')`).
   - Smooth SVG area sparkline with >= 30 real data points.
   - Market hours validator (Mon-Fri 09:15-15:30 IST).
4. **Page Implementations:**
   - `index.html`: Logged-out honest hero vs logged-in trading dashboard.
   - `market.html`: Table-first watchlist with cards/heatmap view toggle, quick row actions, and column filters.
   - `stock.html`: 2-pane TradingView candlestick chart with HUD, volume, MA overlays, and inline order panel.
   - `portfolio.html`: Summary strip, unclipped holdings table, comparative performance area chart vs Nifty, large donut with center value, and horizontal P&L bars.
   - `trade.html`: Full-page order desk matching the drawer component.
   - `history.html`: Order book with status chips, date/symbol filters, and CSV export.
   - `login.html` & `register.html`: Clean two-column layout with live market snapshot.
   - `admin.html`: Dense table-first administrative panel with action reason modal.
5. **Critique & Visual Polish:**
   - Multi-viewport verification (1440px desktop, 1024px tablet, 390px mobile).
   - Elimination of unnecessary decoration.
   - Static resources synchronization to `src/main/resources/static`.
