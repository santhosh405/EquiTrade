function showToast(message, type = 'info') {
  let toastStack = document.getElementById('toastStack');
  if (!toastStack) {
    toastStack = document.createElement('div');
    toastStack.id = 'toastStack';
    toastStack.className = 'toast-stack';
    document.body.appendChild(toastStack);
  }

  const icons = {
    success: '✓',
    error: '✕',
    info: 'ℹ',
    warning: '⚠'
  };

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `<span style="font-weight:700; margin-right:6px;">${icons[type] || 'ℹ'}</span><span>${message}</span>`;
  toastStack.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(-10px)';
    setTimeout(() => toast.remove(), 250);
  }, 2800);
}

function showPageLoader() {
  let loader = document.getElementById('pageLoader');
  if (!loader) {
    loader = document.createElement('div');
    loader.id = 'pageLoader';
    loader.className = 'page-loader';
    loader.innerHTML = '<div class="loader"></div>';
    document.body.appendChild(loader);
  }
  loader.classList.remove('hidden');
}

function hidePageLoader() {
  const loader = document.getElementById('pageLoader');
  if (loader) loader.classList.add('hidden');
}

function renderEmptyState(message, actionHtml = '') {
  return `
    <div class="empty-state" style="padding: 36px 20px; text-align: center;">
      <div style="font-size: 2rem; margin-bottom: 10px; opacity: 0.6;">🔍</div>
      <div style="font-weight: 600; margin-bottom: 8px;">${message}</div>
      ${actionHtml}
    </div>
  `;
}

function renderErrorState(message) {
  return `
    <div class="error-state" style="padding: 24px; text-align: center; border-radius: var(--radius-sm); background: var(--danger-soft); color: var(--danger); border: 1px solid rgba(248, 113, 113, 0.3);">
      <div style="font-weight: 700; margin-bottom: 4px;">Error Encountered</div>
      <div>${message}</div>
    </div>
  `;
}

function getSafetyColor(value) {
  return Number(value) >= 0 ? 'positive' : 'negative';
}

function makeSparkline(values, positive = true) {
  return createSparkline(values, positive);
}

function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (!modal) return;
  modal.classList.add('open');
  document.body.style.overflow = 'hidden';
}

function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (!modal) return;
  modal.classList.remove('open');
  document.body.style.overflow = '';
}

function bindModalClose(modalId) {
  const modal = document.getElementById(modalId);
  if (!modal) return;

  const backdrop = modal.closest('.modal-backdrop') || modal;

  backdrop.addEventListener('click', (event) => {
    if (event.target === backdrop) {
      closeModal(modalId);
    }
  });

  const closeButtons = modal.querySelectorAll('[data-close-modal]');
  closeButtons.forEach(btn => {
    btn.addEventListener('click', () => closeModal(modalId));
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && modal.classList.contains('open')) {
      closeModal(modalId);
    }
  });
}

function animateValue(element, target, prefix = '', suffix = '', digits = 2) {
  const value = Number(target || 0);
  const duration = 750;
  const start = performance.now();

  function tick(now) {
    const progress = Math.min((now - start) / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    const current = value * eased;
    element.textContent = `${prefix}${current.toLocaleString('en-IN', {
      minimumFractionDigits: digits,
      maximumFractionDigits: digits,
    })}${suffix}`;
    if (progress < 1) {
      requestAnimationFrame(tick);
    } else {
      element.textContent = `${prefix}${value.toLocaleString('en-IN', {
        minimumFractionDigits: digits,
        maximumFractionDigits: digits,
      })}${suffix}`;
    }
  }

  requestAnimationFrame(tick);
}
