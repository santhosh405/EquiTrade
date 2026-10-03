const API_BASE_URL = (window.location.protocol.startsWith('http') && window.location.port !== '8080')
  ? 'http://localhost:8080'
  : '';

function getToken() {
  return localStorage.getItem('equitrade_token');
}

function setToken(token) {
  if (token) {
    localStorage.setItem('equitrade_token', token);
  } else {
    localStorage.removeItem('equitrade_token');
  }
}

async function apiRequest(path, options = {}) {
  const { method = 'GET', body, headers = {}, requireAuth = false } = options;
  const token = getToken();

  const requestHeaders = {
    Accept: 'application/json',
    ...headers,
  };

  if (!(body instanceof FormData)) {
    requestHeaders['Content-Type'] = requestHeaders['Content-Type'] || 'application/json';
  }

  if (token) {
    requestHeaders.Authorization = `Bearer ${token}`;
  }

  const requestOptions = {
    method,
    headers: requestHeaders,
  };

  if (body !== undefined && body !== null) {
    requestOptions.body = body instanceof FormData ? body : JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, requestOptions);
  } catch (netErr) {
    throw new Error('Network error: Unable to connect to EquiTrade backend. Please ensure the server is running on port 8080.');
  }

  if (response.status === 401 || response.status === 403) {
    if (requireAuth || (!window.location.pathname.endsWith('login.html') && !window.location.pathname.endsWith('register.html') && !window.location.pathname.endsWith('index.html') && !window.location.pathname.endsWith('market.html') && !window.location.pathname.endsWith('stock.html') && !window.location.pathname.endsWith('404.html'))) {
      if (typeof logoutSession === 'function') logoutSession();
      if (!window.location.pathname.endsWith('login.html') && !window.location.pathname.endsWith('register.html')) {
        window.location.href = 'login.html';
      }
    }
  }

  const isJsonResponse = response.headers.get('content-type')?.includes('application/json');
  const payload = isJsonResponse ? await response.json() : null;

  if (!response.ok) {
    const message = payload?.message || payload?.error || 'Something went wrong';
    throw new Error(message);
  }

  return payload;
}

const apiGet = (path, options = {}) => apiRequest(path, { ...options, method: 'GET' });
const apiPost = (path, body, options = {}) => apiRequest(path, { ...options, method: 'POST', body });
const apiPut = (path, body, options = {}) => apiRequest(path, { ...options, method: 'PUT', body });
const apiDelete = (path, options = {}) => apiRequest(path, { ...options, method: 'DELETE' });

function normalizeAmount(value) {
  const numeric = Number(value || 0);
  return Number.isFinite(numeric) ? numeric : 0;
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(normalizeAmount(value));
}

function formatPercent(value) {
  const val = normalizeAmount(value);
  return `${val >= 0 ? '+' : ''}${val.toFixed(2)}%`;
}

function formatNumber(value, digits = 2) {
  return new Intl.NumberFormat('en-IN', {
    maximumFractionDigits: digits,
    minimumFractionDigits: digits,
  }).format(normalizeAmount(value));
}

function createSparkline(values, positive = true) {
  const safeValues = (values && values.length) ? values : [50, 54, 52, 60, 58, 66, 64, 68];
  const min = Math.min(...safeValues);
  const max = Math.max(...safeValues);
  const range = max - min || 1;

  const points = safeValues.map((value, index) => {
    const x = (index / (safeValues.length - 1)) * 100;
    const y = 100 - ((value - min) / range) * 75 - 12;
    return `${x.toFixed(1)},${y.toFixed(1)}`;
  }).join(' ');

  const stroke = positive ? '#22c55e' : '#f87171';
  return `
    <svg class="sparkline" viewBox="0 0 100 100" preserveAspectRatio="none" aria-label="Price trend">
      <polyline fill="none" stroke="${stroke}" stroke-width="4" points="${points}" stroke-linecap="round" stroke-linejoin="round" />
    </svg>
  `;
}
