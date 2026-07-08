
const registerForm = document.getElementById('registerForm');
const loginForm = document.getElementById('loginForm');
const buyForm = document.getElementById('buyForm');
const stockList = document.getElementById('stockList');
const portfolioList = document.getElementById('portfolioList');
const adminStockList = document.getElementById('adminStockList');
const refreshStocksBtn = document.getElementById('refreshStocksBtn');
const loadAdminStocksBtn = document.getElementById('loadAdminStocksBtn');
const viewPortfolioBtn = document.getElementById('viewPortfolioBtn');
const stockSearch = document.getElementById('stockSearch');
const stockCount = document.getElementById('stockCount');
const tradeMessage = document.getElementById('tradeMessage');

const sampleStocks = [
  { id: 1, symbol: 'AAPL', companyName: 'Apple Inc.', price: 174.55 },
  { id: 2, symbol: 'MSFT', companyName: 'Microsoft Corp.', price: 335.20 },
  { id: 3, symbol: 'GOOGL', companyName: 'Alphabet Inc.', price: 131.80 },
  { id: 4, symbol: 'AMZN', companyName: 'Amazon.com, Inc.', price: 160.92 },
  { id: 5, symbol: 'TSLA', companyName: 'Tesla, Inc.', price: 275.30 },
  { id: 6, symbol: 'NVDA', companyName: 'NVIDIA Corp.', price: 107.85 },
  { id: 7, symbol: 'META', companyName: 'Meta Platforms, Inc.', price: 315.14 },
  { id: 8, symbol: 'NFLX', companyName: 'Netflix, Inc.', price: 536.20 },
  { id: 9, symbol: 'ADBE', companyName: 'Adobe Inc.', price: 570.00 },
  { id: 10, symbol: 'INTC', companyName: 'Intel Corp.', price: 35.26 },
  { id: 11, symbol: 'CSCO', companyName: 'Cisco Systems, Inc.', price: 52.34 },
  { id: 12, symbol: 'ORCL', companyName: 'Oracle Corp.', price: 107.50 },
  { id: 13, symbol: 'CRM', companyName: 'Salesforce, Inc.', price: 212.90 },
  { id: 14, symbol: 'PYPL', companyName: 'PayPal Holdings, Inc.', price: 64.12 },
  { id: 15, symbol: 'UBER', companyName: 'Uber Technologies Inc.', price: 33.07 },
  { id: 16, symbol: 'BABA', companyName: 'Alibaba Group Holding Ltd.', price: 88.45 },
  { id: 17, symbol: 'SQ', companyName: 'Block, Inc.', price: 63.80 },
  { id: 18, symbol: 'SPOT', companyName: 'Spotify Technology S.A.', price: 219.05 },
  { id: 19, symbol: 'AMD', companyName: 'Advanced Micro Devices, Inc.', price: 115.72 },
  { id: 20, symbol: 'DIS', companyName: 'The Walt Disney Company', price: 96.84 },
];

let allStocks = [];
let userPurchases = {};

async function request(path, options = {}) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function displayMessage(message, isError = false) {
  if (!tradeMessage) return;
  tradeMessage.textContent = message;
  tradeMessage.style.color = isError ? '#b91c1c' : '#2563eb';
}

function formatPrice(value) {
  if (typeof value !== 'number') return value;
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(value);
}

function createStockCard(stock) {
  return `
    <article class="market-card">
      <div>
        <p class="stat-label">${stock.symbol} · ID ${stock.id}</p>
        <h3>${stock.companyName}</h3>
      </div>
      <div>
        <p class="price">${formatPrice(stock.price)}</p>
        <button type="button" class="secondary" onclick="prefillBuyForm(${stock.id})">Buy stock</button>
      </div>
    </article>
  `;
}

async function loadStocks() {
  const stocks = await request('/stocks');
  allStocks = Array.isArray(stocks) && stocks.length ? stocks : sampleStocks;
  if (stockCount) stockCount.textContent = allStocks.length;
  renderStockList();
}

function renderStockList() {
  const query = stockSearch?.value.trim().toLowerCase() || '';
  const filtered = allStocks.filter((stock) => {
    return stock.symbol.toLowerCase().includes(query) || stock.companyName.toLowerCase().includes(query);
  });
  if (!filtered.length) {
    stockList.innerHTML = '<p>No matching stocks found.</p>';
    return;
  }
  stockList.innerHTML = filtered.map(createStockCard).join('');
}

function prefillBuyForm(stockId) {
  const input = document.getElementById('buyStockId');
  if (input) input.value = stockId;
  displayMessage('Stock selected. Enter quantity and buy.', false);
}

registerForm?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const username = document.getElementById('registerUsername').value.trim();
  const email = document.getElementById('registerEmail').value.trim();
  const password = document.getElementById('registerPassword').value;
  const result = await request('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username, email, password }),
  });
  displayMessage(result || 'Registration complete.');
});

loginForm?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const email = document.getElementById('loginEmail').value.trim();
  const password = document.getElementById('loginPassword').value;
  const result = await request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
  displayMessage(result || 'Login complete.');
});

buyForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const userId = Number(document.getElementById('userIdInput').value);
  const stockId = Number(document.getElementById('buyStockId').value);
  const quantity = Number(document.getElementById('buyQuantity').value);
  if (!userId || !stockId || !quantity) {
    displayMessage('Enter valid values for User ID, Stock ID, and quantity.', true);
    return;
  }
  const result = await request('/trade/buy', {
    method: 'POST',
    body: JSON.stringify({ userId, stockId, quantity }),
  });
  const isError = typeof result === 'string' && /insufficient|not found|invalid/i.test(result);
  displayMessage(result || 'Trade completed.', isError);
  
  if (!isError) {
    const stock = allStocks.find(s => s.id === stockId);
    if (stock) {
      if (!userPurchases[userId]) userPurchases[userId] = {};
      userPurchases[userId][stockId] = (userPurchases[userId][stockId] || 0) + quantity;
    }
  }
  
  await viewPortfolio();
});

async function viewPortfolio() {
  const userId = Number(document.getElementById('userIdInput').value);
  if (!userId) {
    portfolioList.innerHTML = '<tr><td colspan="3">Enter a valid User ID to view holdings.</td></tr>';
    return;
  }
  const portfolio = await request(`/portfolio/${userId}`);
  
  let holdings = [];
  if (Array.isArray(portfolio) && portfolio.length) {
    holdings = portfolio;
  } else if (userPurchases[userId] && Object.keys(userPurchases[userId]).length) {
    holdings = Object.entries(userPurchases[userId]).map(([stockId, qty]) => {
      const stock = allStocks.find(s => s.id === Number(stockId));
      return { quantity: qty, stock: stock || { companyName: 'Unknown', symbol: 'N/A' } };
    });
  }
  
  if (!holdings.length) {
    portfolioList.innerHTML = '<tr><td colspan="3">No holdings found for this user.</td></tr>';
    return;
  }
  portfolioList.innerHTML = holdings
    .map((item) => `
      <tr>
        <td>${item.stock?.companyName || 'Unknown'}</td>
        <td>${item.stock?.symbol || 'N/A'}</td>
        <td>${item.quantity}</td>
      </tr>
    `)
    .join('');
}

async function loadAdminStocks() {
  const stocks = await request('/admin/stocks');
  const adminStocks = Array.isArray(stocks) && stocks.length ? stocks : sampleStocks;
  if (!adminStocks.length) {
    adminStockList.innerHTML = '<p>No admin stocks available.</p>';
    return;
  }
  adminStockList.innerHTML = adminStocks
    .map((stock) => `
      <article class="market-card">
        <div>
          <p class="stat-label">${stock.symbol}</p>
          <h3>${stock.companyName}</h3>
        </div>
        <div>
          <p class="price">${formatPrice(stock.price)}</p>
          <button type="button" class="secondary" onclick="deleteStock(${stock.id})">Delete</button>
        </div>
      </article>
    `)
    .join('');
}

async function deleteStock(id) {
  const result = await request(`/admin/stock/${id}`, { method: 'DELETE' });
  displayMessage(result || 'Stock removed.');
  await loadAdminStocks();
}

refreshStocksBtn.addEventListener('click', loadStocks);
loadAdminStocksBtn.addEventListener('click', loadAdminStocks);
viewPortfolioBtn.addEventListener('click', viewPortfolio);
stockSearch?.addEventListener('input', renderStockList);
window.prefillBuyForm = prefillBuyForm;
window.deleteStock = deleteStock;
window.addEventListener('DOMContentLoaded', async () => {
  await loadStocks();
  await loadAdminStocks();
});
