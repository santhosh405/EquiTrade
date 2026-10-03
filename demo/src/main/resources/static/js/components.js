/**
 * EquiTrade Global UI Components
 * Institutional-grade Navigation, Live Index Tape, Global Order Drawer, and Mobile Tab Bar.
 */

// Market Hours Calculation (NSE Mon-Fri 09:15 - 15:30 IST)
function getNSEMarketStatus() {
  const now = new Date();
  // Convert current time to IST (UTC + 5:30)
  const utc = now.getTime() + (now.getTimezoneOffset() * 60000);
  const ist = new Date(utc + (3600000 * 5.5));
  
  const day = ist.getDay(); // 0 is Sunday, 6 is Saturday
  const hours = ist.getHours();
  const minutes = ist.getMinutes();
  const timeInMinutes = hours * 60 + minutes;

  // Trading hours: 09:15 (555 min) to 15:30 (930 min)
  const isWeekday = day >= 1 && day <= 5;
  const isMarketHours = timeInMinutes >= 555 && timeInMinutes <= 930;

  if (isWeekday && isMarketHours) {
    return { isOpen: true, text: 'Market open' };
  } else {
    return { isOpen: false, text: 'Market closed · Opens 9:15 am IST' };
  }
}

// Global Order Drawer State
window.activeOrderStock = null;
window.activeOrderAction = 'buy';

document.addEventListener('DOMContentLoaded', () => {
  // 1. Initialize Light/Dark theme from localStorage (Light by default)
  const savedTheme = localStorage.getItem('equitrade_theme');
  if (savedTheme === 'dark') {
    document.body.classList.add('dark');
  } else {
    document.body.classList.remove('dark');
  }

  const pageName = (document.body.dataset.page || 'home').toLowerCase();
  const user = typeof getStoredUser === 'function' ? getStoredUser() : null;
  const userIsAdmin = typeof isAdmin === 'function' && isAdmin();

  // 2. Build Top Navigation Bar
  const nav = document.createElement('header');
  nav.className = 'topbar';
  nav.innerHTML = `
    <div class="container nav-inner">
      <a href="index.html" class="brand" aria-label="EquiTrade Home">
        <span class="brand-icon">EQ</span>
        <span>EquiTrade</span>
      </a>

      <nav class="nav-links" id="navLinks" aria-label="Primary Navigation">
        <a class="nav-link ${pageName === 'home' ? 'active' : ''}" href="index.html">Home</a>
        <a class="nav-link ${pageName === 'market' ? 'active' : ''}" href="market.html">Markets</a>
        <a class="nav-link ${pageName === 'portfolio' ? 'active' : ''}" href="portfolio.html">Portfolio</a>
        <a class="nav-link ${pageName === 'history' ? 'active' : ''}" href="history.html">Orders</a>
        <a class="nav-link ${pageName === 'trade' ? 'active' : ''}" href="trade.html">Trade Desk</a>
        ${userIsAdmin ? `<a class="nav-link ${pageName === 'admin' ? 'active' : ''}" href="admin.html">Admin</a>` : ''}
      </nav>

      <div class="header-search">
        <div class="search-input-wrap">
          <svg class="search-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input id="globalSearchInput" type="text" placeholder="Search stocks (e.g. TCS)..." aria-label="Search securities" autocomplete="off" />
          <kbd class="search-kbd">/</kbd>
        </div>
        <div id="searchSuggestions" class="search-suggestions" role="listbox"></div>
      </div>

      <div class="auth-actions" id="authNav">
        ${user ? `
          <div class="wallet-badge" title="Practice Cash Balance">
            <span class="wallet-label">Cash</span>
            <span class="wallet-val user-balance">${typeof formatCurrency === 'function' ? formatCurrency(user.balance || 0) : '₹' + (user.balance || 0)}</span>
          </div>
        ` : ''}

        <button class="theme-toggle-btn" id="themeToggleBtn" type="button" aria-label="Toggle dark mode" title="Toggle theme">
          ${document.body.classList.contains('dark') ? '☀️' : '🌙'}
        </button>

        ${user ? `
          <button class="btn btn-secondary btn-sm" id="logoutBtn" type="button">Logout</button>
        ` : `
          <a class="btn btn-secondary btn-sm" href="login.html">Login</a>
          <a class="btn btn-primary btn-sm" href="register.html">Register</a>
        `}
      </div>
    </div>
  `;
  document.body.insertBefore(nav, document.body.firstChild);

  // 3. Build Live Index & Market Tape Bar
  const tape = document.createElement('div');
  tape.className = 'tape-bar';
  const marketStatus = getNSEMarketStatus();
  tape.innerHTML = `
    <div class="container tape-inner">
      <div class="market-status-badge">
        <span class="status-dot ${marketStatus.isOpen ? 'open' : ''}"></span>
        <span>${marketStatus.text}</span>
      </div>

      <div class="tape-items-wrap" id="tapeItems">
        <div class="tape-item" data-symbol="NIFTY 50">
          <span class="tape-symbol">NIFTY 50</span>
          <span class="tape-price">24,850.30</span>
          <span class="tape-change pos">+0.42%</span>
        </div>
        <div class="tape-item" data-symbol="SENSEX">
          <span class="tape-symbol">SENSEX</span>
          <span class="tape-price">81,420.15</span>
          <span class="tape-change pos">+0.38%</span>
        </div>
        <div class="tape-item" data-symbol="BANK NIFTY">
          <span class="tape-symbol">BANK NIFTY</span>
          <span class="tape-price">52,180.90</span>
          <span class="tape-change neg">-0.15%</span>
        </div>
      </div>
    </div>
  `;
  nav.parentNode.insertBefore(tape, nav.nextSibling);

  // Load dynamic stock movers into tape
  loadTapeMovers();

  // 4. Build Global Slide-Over Order Drawer
  const drawerMarkup = `
    <div class="drawer-backdrop" id="globalDrawerBackdrop" aria-hidden="true">
      <div class="order-drawer" id="globalOrderDrawer" role="dialog" aria-modal="true" aria-labelledby="drawerSymbolTitle">
        <div class="drawer-header">
          <div>
            <div class="drawer-stock-info">
              <span class="drawer-symbol" id="drawerSymbol">TCS</span>
              <span class="stock-chip positive" id="drawerChangeChip">+0.00%</span>
            </div>
            <div style="font-size: var(--font-size-xs); color: var(--color-slate); margin-top: 2px;" id="drawerCompanyName">Tata Consultancy Services</div>
          </div>
          <button class="drawer-close" id="drawerCloseBtn" type="button" aria-label="Close order drawer">✕</button>
        </div>

        <div class="drawer-body">
          <!-- Buy / Sell Segmented Switch -->
          <div class="order-action-switch">
            <button type="button" class="switch-btn active-buy" id="drawerActionBuy">BUY</button>
            <button type="button" class="switch-btn" id="drawerActionSell">SELL</button>
          </div>

          <!-- Quantity Input & Quick Increment Pills -->
          <div class="form-group">
            <div style="display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 4px;">
              <label for="drawerQty" style="margin-bottom:0;">Order Quantity (Shares)</label>
              <span style="font-size: var(--font-size-xs); color: var(--color-slate);" id="drawerHoldingNote">Holding: 0 shares</span>
            </div>
            <input type="number" id="drawerQty" class="text-input" min="1" step="1" value="1" />
            <div class="qty-pills">
              <button type="button" class="qty-pill" data-qty-add="1">+1</button>
              <button type="button" class="qty-pill" data-qty-add="5">+5</button>
              <button type="button" class="qty-pill" data-qty-add="10">+10</button>
              <button type="button" class="qty-pill" data-qty-add="25">+25</button>
              <button type="button" class="qty-pill" data-qty-add="50">+50</button>
              <button type="button" class="qty-pill" data-qty-add="100">+100</button>
            </div>
          </div>

          <!-- Order Summary Breakdown -->
          <div class="order-summary-box">
            <div class="summary-row">
              <span>Execution Price</span>
              <strong id="drawerExecPrice">₹0.00</strong>
            </div>
            <div class="summary-row">
              <span>Brokerage</span>
              <span>₹0.00 (Zero Practice Fee)</span>
            </div>
            <div class="summary-row">
              <span>Estimated Taxes & STT</span>
              <span>₹0.00</span>
            </div>
            <div class="summary-row">
              <span>Available Cash</span>
              <strong id="drawerAvailableCash">₹0.00</strong>
            </div>
            <div class="summary-row">
              <span>Total Order Value</span>
              <strong id="drawerTotalValue" style="color: var(--color-action);">₹0.00</strong>
            </div>
          </div>

          <!-- In-Context Alert Box -->
          <div id="drawerAlert" style="display: none; padding: 10px 12px; border-radius: var(--radius-sm); font-size: var(--font-size-xs); margin-bottom: 16px;"></div>
        </div>

        <div class="drawer-footer">
          <button type="button" class="btn btn-primary btn-order-submit" id="drawerSubmitBtn">
            Place Buy Order
          </button>
        </div>
      </div>
    </div>
  `;
  document.body.insertAdjacentHTML('beforeend', drawerMarkup);

  // 5. Build Mobile Bottom Navigation Bar (<768px)
  const mobileNav = document.createElement('nav');
  mobileNav.className = 'mobile-nav-bar';
  mobileNav.innerHTML = `
    <a href="index.html" class="mobile-nav-item ${pageName === 'home' ? 'active' : ''}">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path></svg>
      <span>Home</span>
    </a>
    <a href="market.html" class="mobile-nav-item ${pageName === 'market' ? 'active' : ''}">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>
      <span>Markets</span>
    </a>
    <a href="portfolio.html" class="mobile-nav-item ${pageName === 'portfolio' ? 'active' : ''}">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21.21 15.89A10 10 0 1 1 8 2.83"></path><path d="M22 12A10 10 0 0 0 12 2v10z"></path></svg>
      <span>Portfolio</span>
    </a>
    <a href="history.html" class="mobile-nav-item ${pageName === 'history' ? 'active' : ''}">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 14 14"></polyline></svg>
      <span>Orders</span>
    </a>
  `;
  document.body.appendChild(mobileNav);

  // 6. Build Footer
  const footer = document.createElement('footer');
  footer.className = 'footer-shell';
  footer.innerHTML = `
    <div class="container footer-inner">
      <div>
        <div style="font-weight: var(--font-weight-semibold); color: var(--color-ink); margin-bottom: 2px;">
          EquiTrade · Virtual Equity Trading Simulator
        </div>
        <div>Simulated NSE execution desk with ₹5,00,000 practice capital. Practice trading with zero financial risk.</div>
      </div>
      <div class="footer-links">
        <a href="market.html">Markets</a>
        <a href="portfolio.html">Portfolio</a>
        <a href="history.html">Orders</a>
        <a href="trade.html">Trade Desk</a>
        ${userIsAdmin ? '<a href="admin.html">Admin</a>' : ''}
      </div>
    </div>
  `;
  document.body.appendChild(footer);

  // 7. Event Listeners & Behaviors
  bindGlobalEvents();
  initDrawerLogic();

  // Refresh balance if logged in
  if (user && typeof refreshCurrentUserBalance === 'function') {
    refreshCurrentUserBalance();
  }
});

// Dynamic Mover Fetch for Tape
async function loadTapeMovers() {
  try {
    const tapeWrap = document.getElementById('tapeItems');
    if (!tapeWrap) return;
    const stocks = await apiGet('/api/stocks/live');
    if (!stocks || !stocks.length) return;

    // Pick top 5 active movers
    const movers = stocks.slice(0, 6);
    const extraItems = movers.map(stock => {
      const pos = Number(stock.changePercent) >= 0;
      return `
        <div class="tape-item" data-symbol="${stock.symbol}" style="cursor: pointer;">
          <span class="tape-symbol">${stock.symbol}</span>
          <span class="tape-price">${typeof formatCurrency === 'function' ? formatCurrency(stock.price) : '₹' + stock.price}</span>
          <span class="tape-change ${pos ? 'pos' : 'neg'}">${pos ? '+' : ''}${Number(stock.changePercent).toFixed(2)}%</span>
        </div>
      `;
    }).join('');

    tapeWrap.insertAdjacentHTML('beforeend', extraItems);

    // Clicking any tape item navigates to stock detail
    tapeWrap.querySelectorAll('.tape-item').forEach(item => {
      item.addEventListener('click', () => {
        const sym = item.dataset.symbol;
        if (sym && !['NIFTY 50', 'SENSEX', 'BANK NIFTY'].includes(sym)) {
          window.location.href = `stock.html?symbol=${encodeURIComponent(sym)}`;
        }
      });
    });
  } catch (e) {
    // Non-blocking tape error fallback
  }
}

// Global Event Binding
function bindGlobalEvents() {
  // Theme Toggle
  const themeBtn = document.getElementById('themeToggleBtn');
  if (themeBtn) {
    themeBtn.addEventListener('click', () => {
      document.body.classList.toggle('dark');
      const isDark = document.body.classList.contains('dark');
      localStorage.setItem('equitrade_theme', isDark ? 'dark' : 'light');
      themeBtn.textContent = isDark ? '☀️' : '🌙';
      // Dispatch theme change event for charts
      window.dispatchEvent(new CustomEvent('themeChanged', { detail: { theme: isDark ? 'dark' : 'light' } }));
    });
  }

  // Logout
  document.getElementById('logoutBtn')?.addEventListener('click', () => {
    if (typeof logoutSession === 'function') logoutSession();
    if (typeof showToast === 'function') showToast('Logged out successfully', 'info');
    setTimeout(() => {
      window.location.href = 'login.html';
    }, 300);
  });

  // Global Search Autocomplete & Keyboard "/" Shortcut
  const searchInput = document.getElementById('globalSearchInput');
  const suggestionBox = document.getElementById('searchSuggestions');

  if (searchInput && suggestionBox) {
    let debounceTimer;
    searchInput.addEventListener('input', (e) => {
      clearTimeout(debounceTimer);
      const query = e.target.value.trim();
      if (!query) {
        suggestionBox.classList.remove('visible');
        suggestionBox.innerHTML = '';
        return;
      }

      debounceTimer = setTimeout(async () => {
        try {
          const results = await apiGet(`/api/stocks/search?q=${encodeURIComponent(query)}`);
          if (!results || !results.length) {
            suggestionBox.innerHTML = '<div class="search-suggestion-item"><span style="color:var(--color-slate); font-size:var(--font-size-xs);">No matching securities found</span></div>';
            suggestionBox.classList.add('visible');
            return;
          }

          suggestionBox.innerHTML = results.slice(0, 6).map(s => {
            const pos = Number(s.changePercent) >= 0;
            return `
              <div class="search-suggestion-item" data-symbol="${s.symbol}">
                <div>
                  <div style="font-weight: var(--font-weight-semibold);">${s.symbol}</div>
                  <div style="font-size: var(--font-size-xs); color: var(--color-slate);">${s.companyName}</div>
                </div>
                <div style="text-align: right;">
                  <div style="font-weight: var(--font-weight-semibold); font-size: var(--font-size-sm);">${typeof formatCurrency === 'function' ? formatCurrency(s.price) : '₹' + s.price}</div>
                  <span class="stock-chip ${pos ? 'positive' : 'negative'}">
                    ${pos ? '+' : ''}${Number(s.changePercent).toFixed(2)}%
                  </span>
                </div>
              </div>
            `;
          }).join('');

          suggestionBox.querySelectorAll('.search-suggestion-item').forEach(el => {
            el.addEventListener('click', () => {
              const sym = el.dataset.symbol;
              if (sym) {
                suggestionBox.classList.remove('visible');
                window.location.href = `stock.html?symbol=${encodeURIComponent(sym)}`;
              }
            });
          });

          suggestionBox.classList.add('visible');
        } catch (err) {
          suggestionBox.classList.remove('visible');
        }
      }, 180);
    });

    // Close suggestion box on outside click
    document.addEventListener('click', (e) => {
      if (!e.target.closest('.header-search')) {
        suggestionBox.classList.remove('visible');
      }
    });
  }

  // Keyboard Shortcuts: "/" search, "Esc" close drawer, "B" / "S"
  document.addEventListener('keydown', (e) => {
    if (e.key === '/' && document.activeElement !== searchInput && !['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement.tagName)) {
      e.preventDefault();
      searchInput?.focus();
    } else if (e.key === 'Escape') {
      closeOrderDrawer();
      suggestionBox?.classList.remove('visible');
    }
  });

  // Global click delegate for any Buy or Sell button
  document.addEventListener('click', (e) => {
    const buyBtn = e.target.closest('.btn-buy, [data-action="buy"]');
    const sellBtn = e.target.closest('.btn-sell, [data-action="sell"]');

    if (buyBtn) {
      e.preventDefault();
      const symbol = buyBtn.dataset.symbol || buyBtn.closest('[data-symbol]')?.dataset.symbol;
      const stockId = buyBtn.dataset.stockId || buyBtn.dataset.id || buyBtn.closest('[data-stock-id]')?.dataset.stockId || buyBtn.closest('[data-id]')?.dataset.id;
      const identifier = symbol || stockId;

      if (!identifier || identifier === 'undefined' || identifier === 'null') {
        showToast('Could not find this stock. Refresh and try again', 'error');
        return;
      }
      window.openOrderDrawer(identifier, 'buy', stockId);
    } else if (sellBtn) {
      e.preventDefault();
      const isPortfolio = document.body.dataset.page === 'portfolio' || window.location.pathname.includes('portfolio');
      const symbol = sellBtn.dataset.symbol || sellBtn.closest('[data-symbol]')?.dataset.symbol;
      const stockId = sellBtn.dataset.stockId || sellBtn.dataset.id || sellBtn.closest('[data-stock-id]')?.dataset.stockId || sellBtn.closest('[data-id]')?.dataset.id;

      if (isPortfolio) {
        const hasValidNumId = stockId && !isNaN(Number(stockId)) && Number(stockId) > 0;
        const hasValidSym = symbol && symbol !== 'undefined' && symbol !== 'null' && symbol.trim().length > 0;
        if (!hasValidNumId && !hasValidSym) {
          showToast('Could not find this holding. Refresh and try again', 'error');
          return;
        }
      }

      const identifier = symbol || stockId;
      if (!identifier || identifier === 'undefined' || identifier === 'null') {
        showToast('Could not find this holding. Refresh and try again', 'error');
        return;
      }
      window.openOrderDrawer(identifier, 'sell', stockId);
    }
  });
}

// Global Order Drawer Operations
function initDrawerLogic() {
  const backdrop = document.getElementById('globalDrawerBackdrop');
  const closeBtn = document.getElementById('drawerCloseBtn');
  const buyBtn = document.getElementById('drawerActionBuy');
  const sellBtn = document.getElementById('drawerActionSell');
  const qtyInput = document.getElementById('drawerQty');
  const submitBtn = document.getElementById('drawerSubmitBtn');

  // Close handlers
  backdrop?.addEventListener('click', (e) => {
    if (e.target === backdrop) closeOrderDrawer();
  });
  closeBtn?.addEventListener('click', closeOrderDrawer);

  // Switch Buy/Sell
  buyBtn?.addEventListener('click', () => setDrawerAction('buy'));
  sellBtn?.addEventListener('click', () => setDrawerAction('sell'));

  // Qty pills
  document.querySelectorAll('.qty-pill').forEach(pill => {
    pill.addEventListener('click', () => {
      const add = Number(pill.dataset.qtyAdd || 1);
      const cur = Number(qtyInput.value || 1);
      qtyInput.value = cur + add;
      recalculateDrawerTotals();
    });
  });

  // Qty input change
  qtyInput?.addEventListener('input', recalculateDrawerTotals);

  // Submit Trade
  submitBtn?.addEventListener('click', executeDrawerOrder);
}

function setDrawerAction(action) {
  window.activeOrderAction = action;
  const buyBtn = document.getElementById('drawerActionBuy');
  const sellBtn = document.getElementById('drawerActionSell');
  const submitBtn = document.getElementById('drawerSubmitBtn');

  if (action === 'buy') {
    buyBtn.className = 'switch-btn active-buy';
    sellBtn.className = 'switch-btn';
    submitBtn.className = 'btn btn-primary btn-order-submit';
    submitBtn.style.backgroundColor = 'var(--color-gain)';
  } else {
    buyBtn.className = 'switch-btn';
    sellBtn.className = 'switch-btn active-sell';
    submitBtn.className = 'btn btn-primary btn-order-submit';
    submitBtn.style.backgroundColor = 'var(--color-loss)';
  }
  recalculateDrawerTotals();
}

window.openOrderDrawer = async function(identifier, action = 'buy', optionalStockId = null) {
  if (!identifier || identifier === 'undefined' || identifier === 'null') {
    showToast(action === 'sell' ? 'Could not find this holding. Refresh and try again' : 'Could not find this stock. Refresh and try again', 'error');
    return;
  }
  if (!isLoggedIn()) {
    showToast('Please sign in to place orders.', 'info');
    setTimeout(() => { window.location.href = 'login.html'; }, 600);
    return;
  }

  try {
    const isNum = !isNaN(Number(identifier)) && Number(identifier) > 0;
    let stockPromise;
    if (isNum) {
      stockPromise = apiGet(`/api/stocks/${Number(identifier)}`);
    } else {
      stockPromise = apiGet(`/api/stocks/symbol/${encodeURIComponent(identifier)}`).catch(() => {
        if (optionalStockId && !isNaN(Number(optionalStockId)) && Number(optionalStockId) > 0) {
          return apiGet(`/api/stocks/${Number(optionalStockId)}`);
        }
        throw new Error(`Stock "${identifier}" not found`);
      });
    }

    const [stock, holdings, profile] = await Promise.all([
      stockPromise,
      apiGet('/api/portfolio/holdings').catch(() => []),
      apiGet('/api/auth/me').catch(() => null)
    ]);

    if (!stock || !stock.id || isNaN(Number(stock.id))) {
      showToast(action === 'sell' ? 'Could not find this holding. Refresh and try again' : 'Could not find this stock. Refresh and try again', 'error');
      return;
    }

    window.activeOrderStock = stock;
    const holding = (holdings || []).find(h => (h.stockSymbol === stock.symbol || h.symbol === stock.symbol || h.stockId === stock.id || h.id === stock.id));
    window.activeOrderHoldingQty = holding ? holding.quantity : 0;
    window.activeUserBalance = profile ? profile.balance : (getStoredUser()?.balance || 0);

    // Populate Drawer UI
    document.getElementById('drawerSymbol').textContent = stock.symbol;
    document.getElementById('drawerCompanyName').textContent = stock.companyName;
    const pos = Number(stock.changePercent) >= 0;
    const chip = document.getElementById('drawerChangeChip');
    chip.className = `stock-chip ${pos ? 'positive' : 'negative'}`;
    chip.textContent = `${pos ? '+' : ''}${Number(stock.changePercent).toFixed(2)}%`;

    document.getElementById('drawerExecPrice').textContent = formatCurrency(stock.price);
    document.getElementById('drawerAvailableCash').textContent = formatCurrency(window.activeUserBalance);
    document.getElementById('drawerHoldingNote').textContent = `Holding: ${window.activeOrderHoldingQty} shares`;

    document.getElementById('drawerQty').value = '1';
    setDrawerAction(action);

    // Open Backdrop
    const backdrop = document.getElementById('globalDrawerBackdrop');
    backdrop.classList.add('open');
    document.body.style.overflow = 'hidden';
  } catch (error) {
    showToast(error.message || 'Unable to load security for order ticket.', 'error');
  }
};

window.closeOrderDrawer = function() {
  const backdrop = document.getElementById('globalDrawerBackdrop');
  if (backdrop) backdrop.classList.remove('open');
  document.body.style.overflow = '';
};

function recalculateDrawerTotals() {
  const stock = window.activeOrderStock;
  if (!stock) return;

  const qty = Math.max(1, parseInt(document.getElementById('drawerQty').value || 1, 10));
  const total = qty * stock.price;
  const isBuy = window.activeOrderAction === 'buy';

  document.getElementById('drawerTotalValue').textContent = formatCurrency(total);
  const submitBtn = document.getElementById('drawerSubmitBtn');
  submitBtn.textContent = `${isBuy ? 'Place Buy Order' : 'Place Sell Order'} (${qty} ${stock.symbol})`;

  const alertBox = document.getElementById('drawerAlert');
  alertBox.style.display = 'none';
  submitBtn.disabled = false;

  // Validation
  if (isBuy && total > window.activeUserBalance) {
    alertBox.style.display = 'block';
    alertBox.style.backgroundColor = 'var(--color-loss-subtle)';
    alertBox.style.color = 'var(--color-loss)';
    alertBox.style.border = '1px solid var(--color-loss-border)';
    alertBox.textContent = `Insufficient cash. Need ${formatCurrency(total - window.activeUserBalance)} more.`;
    submitBtn.disabled = true;
  } else if (!isBuy && qty > (window.activeOrderHoldingQty || 0)) {
    alertBox.style.display = 'block';
    alertBox.style.backgroundColor = 'var(--color-loss-subtle)';
    alertBox.style.color = 'var(--color-loss)';
    alertBox.style.border = '1px solid var(--color-loss-border)';
    alertBox.textContent = `You only own ${window.activeOrderHoldingQty || 0} shares of ${stock.symbol}.`;
    submitBtn.disabled = true;
  }
}

async function executeDrawerOrder() {
  const stock = window.activeOrderStock;
  const action = window.activeOrderAction;
  const qty = parseInt(document.getElementById('drawerQty').value || 1, 10);
  const submitBtn = document.getElementById('drawerSubmitBtn');

  if (!stock || !stock.id || isNaN(Number(stock.id))) {
    showToast('Could not find this holding. Refresh and try again', 'error');
    return;
  }
  if (qty <= 0) {
    showToast('Please enter a valid quantity greater than 0', 'error');
    return;
  }

  try {
    submitBtn.disabled = true;
    submitBtn.textContent = 'Executing Order...';

    const endpoint = action === 'buy' ? '/api/trade/buy' : '/api/trade/sell';
    const response = await apiPost(endpoint, {
      stockId: stock.id,
      quantity: qty
    });

    showToast(`Order executed: ${action.toUpperCase()} ${qty} ${stock.symbol}`, 'success');
    closeOrderDrawer();

    // Update user balance locally and in header
    if (response && response.newBalance !== undefined) {
      const user = getStoredUser() || {};
      user.balance = response.newBalance;
      localStorage.setItem('equitrade_user', JSON.stringify(user));
      const balEl = document.querySelector('.user-balance');
      if (balEl) balEl.textContent = formatCurrency(response.newBalance);
    } else {
      refreshCurrentUserBalance();
    }

    // Fire order executed event for any active page to refresh tables/charts
    window.dispatchEvent(new CustomEvent('orderExecuted', { detail: { symbol: stock.symbol, action, quantity: qty } }));

  } catch (error) {
    showToast(error.message || 'Trade execution failed.', 'error');
  } finally {
    submitBtn.disabled = false;
    recalculateDrawerTotals();
  }
}
