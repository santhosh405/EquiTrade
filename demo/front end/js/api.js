const BACKEND_PORT = '8081';
const API_BASE_URL = (window.location.protocol.startsWith('http') && window.location.port !== BACKEND_PORT)
  ? `http://localhost:${BACKEND_PORT}`
  : (window.location.protocol.startsWith('http') ? '' : `http://localhost:${BACKEND_PORT}`);

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
    throw new Error('Network error: Unable to connect to EquiTrade backend. Please ensure the server is running on port 8081.');
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
  let raw = (Array.isArray(values) && values.length > 0) ? values.map(Number).filter(v => Number.isFinite(v)) : [];
  if (raw.length === 0) {
    // Generate distinct realistic random walk based on direction
    let cur = 100;
    raw = [cur];
    const trend = positive ? 0.35 : -0.35;
    for (let i = 1; i < 30; i++) {
      cur += trend + (Math.random() - 0.48) * 1.5;
      raw.push(cur);
    }
  }

  // Ensure at least 30 data points through smooth linear interpolation
  const targetCount = Math.max(30, raw.length);
  const dataPoints = [];
  if (raw.length < targetCount) {
    for (let i = 0; i < targetCount; i++) {
      const pos = (i / (targetCount - 1)) * (raw.length - 1);
      const lowIndex = Math.floor(pos);
      const highIndex = Math.min(raw.length - 1, Math.ceil(pos));
      const fraction = pos - lowIndex;
      const interpolated = raw[lowIndex] + (raw[highIndex] - raw[lowIndex]) * fraction;
      dataPoints.push(interpolated);
    }
  } else {
    dataPoints.push(...raw);
  }

  const min = Math.min(...dataPoints);
  const max = Math.max(...dataPoints);
  const range = max - min || 1;

  const coords = dataPoints.map((val, idx) => ({
    x: (idx / (dataPoints.length - 1)) * 100,
    y: 88 - ((val - min) / range) * 72
  }));

  // Build Catmull-Rom cubic bezier smooth curve
  let linePath = `M ${coords[0].x.toFixed(2)},${coords[0].y.toFixed(2)}`;
  for (let i = 0; i < coords.length - 1; i++) {
    const p0 = coords[i === 0 ? i : i - 1];
    const p1 = coords[i];
    const p2 = coords[i + 1];
    const p3 = coords[i + 2 < coords.length ? i + 2 : i + 1];

    const cp1x = p1.x + (p2.x - p0.x) / 6;
    const cp1y = p1.y + (p2.y - p0.y) / 6;
    const cp2x = p2.x - (p3.x - p1.x) / 6;
    const cp2y = p2.y - (p3.y - p1.y) / 6;

    linePath += ` C ${cp1x.toFixed(2)},${cp1y.toFixed(2)} ${cp2x.toFixed(2)},${cp2y.toFixed(2)} ${p2.x.toFixed(2)},${p2.y.toFixed(2)}`;
  }

  const lastPt = coords[coords.length - 1];
  const areaPath = `${linePath} L 100,100 L 0,100 Z`;

  // Use exact tokens: Gain #0E9F6E, Loss #D6393A
  const strokeColor = positive ? '#0E9F6E' : '#D6393A';
  const gradId = `sparkGrad_${Math.random().toString(36).substring(2, 9)}`;
  const stopColor = positive ? 'rgba(14, 159, 110, ' : 'rgba(214, 57, 58, ';

  return `
    <div class="market-sparkline-wrap" aria-label="Smooth price trajectory chart" style="display:inline-block; width:110px; height:32px;">
      <svg class="sparkline-svg" viewBox="0 0 100 100" preserveAspectRatio="none" style="width:100%; height:100%;">
        <defs>
          <linearGradient id="${gradId}" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="${stopColor}0.32)" />
            <stop offset="70%" stop-color="${stopColor}0.08)" />
            <stop offset="100%" stop-color="${stopColor}0.0)" />
          </linearGradient>
        </defs>
        <path class="sparkline-area" d="${areaPath}" fill="url(#${gradId})" />
        <path class="sparkline-line" d="${linePath}" fill="none" stroke="${strokeColor}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
        <circle class="sparkline-dot" cx="${lastPt.x.toFixed(2)}" cy="${lastPt.y.toFixed(2)}" r="2.2" fill="${strokeColor}" />
      </svg>
    </div>
  `;
}

