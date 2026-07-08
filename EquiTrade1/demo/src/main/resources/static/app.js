const registerForm = document.getElementById('registerForm');
const loginForm = document.getElementById('loginForm');
const buyForm = document.getElementById('buyForm');
const stockList = document.getElementById('stockList');
const portfolioList = document.getElementById('portfolioList');
const adminStockList = document.getElementById('adminStockList');
const refreshStocksBtn = document.getElementById('refreshStocksBtn');
const loadAdminStocksBtn = document.getElementById('loadAdminStocksBtn');
const viewPortfolioBtn = document.getElementById('viewPortfolioBtn');

async function request(path, options = {}) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  });

  const text = await response.text();
  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function setMessage(element, message, isError = false) {
  element.textContent = message;
  element.style.color = isError ? '#b91c1c' : '#166534';
}

async function loadStocks() {
  const stocks = await request('/stocks');
  if (!Array.isArray(stocks)) {
    stockList.innerHTML = '<p>No stocks available.</p>';
    return;
  }

  stockList.innerHTML = stocks.map(stock => `
    <div class="stock-item">
      <h3>${stock.companyName}</h3>
      <p><strong>Symbol:</strong> ${stock.symbol}</p>
      <p><strong>Price:</strong> $${stock.price}</p>
      <button type="button" onclick="prefillBuyForm(${stock.id})">Buy ${stock.symbol}</button>
    </div>
  `).join('');
}

function prefillBuyForm(stockId) {
  document.getElementById('buyStockId').value = stockId;
}

registerForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const username = document.getElementById('registerUsername').value;
  const email = document.getElementById('registerEmail').value;
  const password = document.getElementById('registerPassword').value;

  const result = await request('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username, email, password })
  });

  setMessage(document.getElementById('registerMessage'), result || 'Registration complete.', false);
});

loginForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const email = document.getElementById('loginEmail').value;
  const password = document.getElementById('loginPassword').value;

  const result = await request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password })
  });

  setMessage(document.getElementById('loginMessage'), result || 'Login complete.', false);
});

buyForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const userId = document.getElementById('userIdInput').value;
  const stockId = document.getElementById('buyStockId').value;
  const quantity = document.getElementById('buyQuantity').value;

  if (!userId) {
    setMessage(document.getElementById('tradeMessage'), 'Enter a user ID before buying.', true);
    return;
  }

  const result = await request('/trade/buy', {
    method: 'POST',
    body: JSON.stringify({ userId: Number(userId), stockId: Number(stockId), quantity: Number(quantity) })
  });

  setMessage(document.getElementById('tradeMessage'), result || 'Trade submitted.', false);
  await viewPortfolio();
});

async function viewPortfolio() {
  const userId = document.getElementById('userIdInput').value;
  if (!userId) {
    portfolioList.innerHTML = '<p>Enter a user ID to view holdings.</p>';
    return;
  }

  const portfolio = await request(`/portfolio/${userId}`);
  if (!portfolio || portfolio.length === 0) {
    portfolioList.innerHTML = '<p>No holdings found.</p>';
    return;
  }

  portfolioList.innerHTML = portfolio.map(item => `
    <div class="portfolio-item">
      <p><strong>Stock:</strong> ${item.stock?.companyName || 'Unknown'}</p>
      <p><strong>Symbol:</strong> ${item.stock?.symbol || '-'}</p>
      <p><strong>Quantity:</strong> ${item.quantity}</p>
    </div>
  `).join('');
}

async function loadAdminStocks() {
  const stocks = await request('/admin/stocks');
  if (!Array.isArray(stocks)) {
    adminStockList.innerHTML = '<p>No admin data available.</p>';
    return;
  }

  adminStockList.innerHTML = stocks.map(stock => `
    <div class="stock-item">
      <h3>${stock.companyName}</h3>
      <p><strong>Symbol:</strong> ${stock.symbol}</p>
      <p><strong>Price:</strong> $${stock.price}</p>
      <button type="button" onclick="deleteStock(${stock.id})">Delete</button>
    </div>
  `).join('');
}

async function deleteStock(id) {
  const result = await request(`/admin/stock/${id}`, { method: 'DELETE' });
  setMessage(document.getElementById('tradeMessage'), result || 'Stock deleted.', false);
  await loadAdminStocks();
}

refreshStocksBtn.addEventListener('click', loadStocks);
loadAdminStocksBtn.addEventListener('click', loadAdminStocks);
viewPortfolioBtn.addEventListener('click', viewPortfolio);

document.addEventListener('DOMContentLoaded', () => {
  loadStocks();
  loadAdminStocks();
  viewPortfolio();
});
