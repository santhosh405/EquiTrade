function getStoredUser() {
  try {
    const value = localStorage.getItem('equitrade_user');
    return value ? JSON.parse(value) : null;
  } catch (error) {
    return null;
  }
}

function saveAuthSession(token, user) {
  setToken(token);
  localStorage.setItem('equitrade_user', JSON.stringify(user));
}

function logoutSession() {
  setToken(null);
  localStorage.removeItem('equitrade_user');
}

function isLoggedIn() {
  return Boolean(getToken()) && Boolean(getStoredUser());
}

function isAdmin() {
  const user = getStoredUser();
  return user?.role === 'ROLE_ADMIN' || user?.role === 'ADMIN';
}

function getCurrentUser() {
  return getStoredUser();
}

async function loginUser(email, password) {
  const response = await apiPost('/api/auth/login', { email, password });
  saveAuthSession(response.token, {
    id: response.id,
    username: response.username,
    email: response.email,
    role: response.role,
    balance: response.balance || 0,
  });
  return response;
}

async function registerUser(username, email, password) {
  return apiPost('/api/auth/register', { username, email, password });
}

async function refreshCurrentUserBalance() {
  if (!isLoggedIn()) return;
  try {
    const profile = await apiGet('/api/auth/me');
    if (profile && profile.balance !== undefined) {
      const user = getStoredUser() || {};
      user.balance = profile.balance;
      localStorage.setItem('equitrade_user', JSON.stringify(user));
      const balanceEl = document.querySelector('.user-balance');
      if (balanceEl) balanceEl.textContent = formatCurrency(profile.balance);
    }
  } catch (err) {
    // Ignore silent refresh error
  }
}

function redirectToLoginIfNeeded() {
  if (!isLoggedIn()) {
    const p = window.location.pathname;
    if (!p.endsWith('login.html') && !p.endsWith('register.html')) {
      window.location.href = 'login.html';
      return true;
    }
  }
  return false;
}

function protectAdminPage() {
  if (!isLoggedIn()) {
    window.location.href = 'login.html';
    return false;
  }

  if (!isAdmin()) {
    showToast('Admin access required. Please login with an administrator account.', 'error');
    setTimeout(() => {
      window.location.href = 'index.html';
    }, 1200);
    return false;
  }

  return true;
}
