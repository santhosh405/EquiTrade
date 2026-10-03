document.addEventListener('DOMContentLoaded', () => {
  // Initialize light/dark theme from localStorage
  const savedTheme = localStorage.getItem('equitrade_theme');
  if (savedTheme === 'light') {
    document.body.classList.add('light');
  }

  const pageName = (document.body.dataset.page || 'home').toLowerCase();
  const user = typeof getStoredUser === 'function' ? getStoredUser() : null;
  const userIsAdmin = typeof isAdmin === 'function' && isAdmin();

  // Create Header / Navigation
  const nav = document.createElement('header');
  nav.className = 'topbar';
  nav.innerHTML = `
    <div class="container nav-inner">
      <a href="index.html" class="brand" aria-label="EquiTrade Home">
        <svg class="brand-mark" width="34" height="34" viewBox="0 0 64 64" fill="none">
          <rect width="64" height="64" rx="16" fill="url(#brandGrad)"/>
          <path d="M18 38 L28 26 L36 34 L46 20" stroke="#ffffff" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"/>
          <circle cx="46" cy="20" r="4.5" fill="#ffffff"/>
          <defs>
            <linearGradient id="brandGrad" x1="0" y1="0" x2="64" y2="64" gradientUnits="userSpaceOnUse">
              <stop stop-color="#4F7CFF"/>
              <stop offset="1" stop-color="#8B5CF6"/>
            </linearGradient>
          </defs>
        </svg>
        <span>EquiTrade</span>
      </a>

      <button class="mobile-toggle" id="mobileToggle" aria-label="Toggle navigation menu">☰</button>

      <nav class="nav-links" id="navLinks">
        <a class="nav-link ${pageName === 'home' ? 'active' : ''}" href="index.html">Home</a>
        <a class="nav-link ${pageName === 'market' ? 'active' : ''}" href="market.html">Market</a>
        <a class="nav-link ${pageName === 'trade' ? 'active' : ''}" href="trade.html">Trade Desk</a>
        <a class="nav-link ${pageName === 'portfolio' ? 'active' : ''}" href="portfolio.html">Portfolio</a>
        <a class="nav-link ${pageName === 'history' ? 'active' : ''}" href="history.html">History</a>
        ${userIsAdmin ? `<a class="nav-link ${pageName === 'admin' ? 'active' : ''}" href="admin.html">Admin</a>` : ''}
      </nav>

      <div class="header-search">
        <span class="search-icon">🔍</span>
        <input id="globalSearchInput" type="text" placeholder="Search stocks (e.g. TCS)..." aria-label="Global stock search" autocomplete="off" />
        <div id="searchSuggestions" class="search-suggestions"></div>
      </div>

      <div class="auth-actions" id="authNav">
        <button class="theme-toggle-btn" id="themeToggleBtn" type="button" aria-label="Toggle color theme" title="Toggle theme">
          ${document.body.classList.contains('light') ? '🌙' : '☀️'}
        </button>
        ${user ? `
          <div class="user-badge" title="Logged in as ${user.username}">
            <span>${user.username}</span>
            <span class="user-balance">${typeof formatCurrency === 'function' ? formatCurrency(user.balance || 0) : '₹' + (user.balance || 0)}</span>
          </div>
          <button class="btn btn-secondary" id="logoutBtn" type="button">Logout</button>
        ` : `
          <a class="btn btn-secondary" href="login.html">Login</a>
          <a class="btn btn-primary" href="register.html">Register</a>
        `}
      </div>
    </div>
  `;

  document.body.insertBefore(nav, document.body.firstChild);

  // Create Footer
  const footer = document.createElement('footer');
  footer.innerHTML = `
    <div class="container footer-inner">
      <div>
        <div style="font-weight: 700; font-size: 1.05rem; margin-bottom: 6px;">EquiTrade · Equity Trading Simulator</div>
        <div style="color: var(--muted); font-size: 0.88rem;">Real-time price simulations, portfolio analytics, and secure order execution.</div>
      </div>
      <div style="display:flex; gap:16px; align-items:center; color:var(--muted); font-size: 0.88rem;">
        <a href="market.html">Market</a>
        <a href="trade.html">Trade</a>
        <a href="portfolio.html">Portfolio</a>
        <a href="history.html">History</a>
        ${userIsAdmin ? '<a href="admin.html">Admin</a>' : ''}
      </div>
    </div>
  `;
  document.body.appendChild(footer);

  // Theme toggle listener
  const themeBtn = document.getElementById('themeToggleBtn');
  if (themeBtn) {
    themeBtn.addEventListener('click', () => {
      document.body.classList.toggle('light');
      const isLight = document.body.classList.contains('light');
      localStorage.setItem('equitrade_theme', isLight ? 'light' : 'dark');
      themeBtn.textContent = isLight ? '🌙' : '☀️';
    });
  }

  // Logout listener
  document.getElementById('logoutBtn')?.addEventListener('click', () => {
    if (typeof logoutSession === 'function') logoutSession();
    if (typeof showToast === 'function') showToast('Logged out successfully', 'info');
    setTimeout(() => {
      window.location.href = 'login.html';
    }, 400);
  });

  // Mobile menu toggle
  document.getElementById('mobileToggle')?.addEventListener('click', () => {
    document.getElementById('navLinks')?.classList.toggle('open');
  });

  // Global search autocomplete
  const searchInput = document.getElementById('globalSearchInput');
  const suggestionBox = document.getElementById('searchSuggestions');

  if (searchInput && suggestionBox) {
    let debounceTimer;
    searchInput.addEventListener('input', (event) => {
      clearTimeout(debounceTimer);
      const query = event.target.value.trim();

      if (!query) {
        suggestionBox.classList.remove('visible');
        suggestionBox.innerHTML = '';
        return;
      }

      debounceTimer = setTimeout(async () => {
        try {
          const results = await apiGet(`/api/stocks/search?q=${encodeURIComponent(query)}`);
          if (!results || !results.length) {
            suggestionBox.innerHTML = '<div class="search-suggestion"><span style="color:var(--muted);">No matching stocks found</span></div>';
            suggestionBox.classList.add('visible');
            return;
          }

          suggestionBox.innerHTML = results.slice(0, 6).map(stock => {
            const pos = Number(stock.changePercent) >= 0;
            return `
              <div class="search-suggestion" data-symbol="${stock.symbol}" style="cursor: pointer;">
                <div>
                  <strong style="color: var(--text);">${stock.symbol}</strong>
                  <div style="color: var(--muted); font-size: 0.78rem;">${stock.companyName}</div>
                </div>
                <div style="text-align: right;">
                  <div style="font-weight: 600; font-size: 0.88rem;">${typeof formatCurrency === 'function' ? formatCurrency(stock.price) : '₹' + stock.price}</div>
                  <span class="stock-chip ${pos ? 'positive' : 'negative'}" style="font-size: 0.72rem; padding: 2px 6px;">
                    ${pos ? '+' : ''}${Number(stock.changePercent || 0).toFixed(2)}%
                  </span>
                </div>
              </div>
            `;
          }).join('');

          suggestionBox.querySelectorAll('.search-suggestion').forEach(item => {
            item.addEventListener('click', () => {
              const symbol = item.dataset.symbol;
              if (symbol) {
                suggestionBox.classList.remove('visible');
                window.location.href = `stock.html?symbol=${encodeURIComponent(symbol)}`;
              }
            });
          });

          suggestionBox.classList.add('visible');
        } catch (error) {
          suggestionBox.classList.remove('visible');
        }
      }, 200);
    });

    document.addEventListener('click', (event) => {
      if (!event.target.closest('.search-suggestion') && !event.target.closest('#globalSearchInput')) {
        suggestionBox.classList.remove('visible');
      }
    });
  }

  // Refresh user balance if logged in
  if (user && typeof refreshCurrentUserBalance === 'function') {
    refreshCurrentUserBalance();
  }
});
