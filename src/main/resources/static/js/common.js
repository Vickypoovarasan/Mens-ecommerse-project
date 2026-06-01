const API_BASE = '';

function getSession() {
  const token = sessionStorage.getItem('accessToken');
  const email = sessionStorage.getItem('userEmail');
  return { token, email, loggedIn: !!token };
}

function setSession(accessToken, email) {
  sessionStorage.setItem('accessToken', accessToken);
  if (email) sessionStorage.setItem('userEmail', email);
}

function clearSession() {
  sessionStorage.removeItem('accessToken');
  sessionStorage.removeItem('userEmail');
}

function updateNavbar() {
  const el = document.getElementById('sessionStatus');
  const logoutBtn = document.getElementById('logoutBtn');
  const loginNavBtn = document.getElementById('loginNavBtn');
  const registerNavBtn = document.getElementById('registerNavBtn');
  const { loggedIn, email } = getSession();

  if (el) {
    if (loggedIn) {
      el.textContent = email ? `Signed in as ${email}` : 'Signed in';
      el.classList.add('active');
    } else {
      el.textContent = 'Guest';
      el.classList.remove('active');
    }
  }

  if (logoutBtn) {
    logoutBtn.style.display = loggedIn ? 'inline-flex' : 'none';
  }
  if (loginNavBtn) {
    loginNavBtn.style.display = loggedIn ? 'none' : 'inline-flex';
  }
  if (registerNavBtn) {
    registerNavBtn.style.display = loggedIn ? 'none' : 'inline-flex';
  }
}

async function logout() {
  try {
    await apiFetch('/api/auth/logout', { method: 'POST' });
  } catch (_) {
    // ignore network or auth errors during logout
  }
  clearSession();
  updateNavbar();
  location.href = 'login.html';
}

async function apiFetch(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (options.body && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }
  const { token } = getSession();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  const res = await fetch(`${API_BASE}${path}`, { ...options, headers, credentials: 'include' });
  if (!res.ok) {
    let msg = res.statusText;
    try {
      const err = await res.json();
      msg = err.message || msg;
    } catch (_) { /* ignore */ }
    if (options.showToast && typeof showToast === 'function') {
      showToast(msg, 'error');
    }
    throw new Error(msg);
  }
  if (res.status === 204) return null;
  return res.json();
}

function formatPrice(amount) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount);
}

const TAX_RATE = 0.12;
const SHIPPING_FLAT = 15;
const FREE_SHIPPING_MIN = 150;

function getCart() {
  try {
    const raw = localStorage.getItem('cart');
    const cart = raw ? JSON.parse(raw) : [];
    return Array.isArray(cart) ? cart : [];
  } catch {
    return [];
  }
}

function saveCart(cart) {
  localStorage.setItem('cart', JSON.stringify(cart));
}

function calcCartTotals(cart, discountAmount = 0) {
  const subtotal = cart.reduce((sum, i) => sum + Number(i.price) * Number(i.quantity), 0);
  const discount = Math.max(0, Number(discountAmount) || 0);
  const discounted = Math.max(0, subtotal - discount);
  const tax = Math.round(discounted * TAX_RATE * 100) / 100;
  const shipping = discounted === 0 || discounted >= FREE_SHIPPING_MIN ? 0 : SHIPPING_FLAT;
  const total = Math.round((discounted + tax + shipping) * 100) / 100;
  return { subtotal, discount, discounted, tax, shipping, total };
}

function escapeHtml(s) {
  const d = document.createElement('div');
  d.textContent = s == null ? '' : String(s);
  return d.innerHTML;
}

function parseJwt(token) {
  try {
    const payload = token.split('.')[1];
    if (!payload) return null;
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(Array.from(atob(base64), c => `%${('00' + c.charCodeAt(0).toString(16)).slice(-2)}`).join(''));
    return JSON.parse(json);
  } catch {
    return null;
  }
}

function getTokenRole(token) {
  const payload = parseJwt(token);
  return payload?.role || null;
}

document.addEventListener('DOMContentLoaded', () => {
  updateNavbar();
  const logoutBtn = document.getElementById('logoutBtn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', logout);
  }
});
