/**
 * Atelier UI helpers — toasts, card messages, page animations.
 * Non-breaking: existing .msg / class names still work; opt-in via setCardMessage / showToast.
 */

function uiEscapeHtml(s) {
  const d = document.createElement('div');
  d.textContent = s == null ? '' : String(s);
  return d.innerHTML;
}

const TOAST_ICONS = { success: '✓', error: '✕', info: 'i', warning: '!' };
const TOAST_TITLES = { success: 'Success', error: 'Error', info: 'Notice', warning: 'Warning' };

function normalizeMsgType(type) {
  if (type === 'ok') return 'success';
  if (type === 'err') return 'error';
  return type || 'info';
}

function ensureToastStack() {
  let stack = document.getElementById('toast-stack');
  if (!stack) {
    stack = document.createElement('div');
    stack.id = 'toast-stack';
    stack.setAttribute('aria-live', 'polite');
    stack.setAttribute('aria-relevant', 'additions');
    document.body.appendChild(stack);
  }
  return stack;
}

/**
 * Floating toast card (top-right).
 * @param {string} message
 * @param {'success'|'error'|'info'|'warning'|'ok'|'err'} type
 * @param {{ title?: string, duration?: number }} options
 */
function showToast(message, type = 'info', options = {}) {
  if (!message) return;
  const t = normalizeMsgType(type);
  const duration = options.duration ?? (t === 'error' ? 5500 : 4200);
  const stack = ensureToastStack();

  const toast = document.createElement('div');
  toast.className = `toast-card toast-card--${t}`;
  toast.setAttribute('role', 'alert');
  toast.innerHTML = `
    <span class="toast-card__icon" aria-hidden="true">${TOAST_ICONS[t] || 'i'}</span>
    <div class="toast-card__body">
      <div class="toast-card__title">${uiEscapeHtml(options.title || TOAST_TITLES[t])}</div>
      <div class="toast-card__text">${uiEscapeHtml(message)}</div>
    </div>
    <button type="button" class="toast-card__close" aria-label="Dismiss">×</button>`;

  const dismiss = () => {
    toast.classList.add('toast-exit');
    toast.addEventListener('animationend', () => toast.remove(), { once: true });
  };

  toast.querySelector('.toast-card__close').addEventListener('click', dismiss);
  stack.appendChild(toast);

  const timer = setTimeout(dismiss, duration);
  toast.addEventListener('mouseenter', () => clearTimeout(timer));
  toast.addEventListener('mouseleave', () => setTimeout(dismiss, 1800));
}

/**
 * Inline card message on an existing element (keeps element id for scripts).
 */
function setCardMessage(el, text, type = 'info') {
  if (!el) return;
  if (!text) {
    el.textContent = '';
    el.className = 'msg';
    return;
  }
  const t = normalizeMsgType(type);
  el.textContent = text;
  el.className = `msg card-msg card-msg--${t}`;
}

function staggerReveal(container, selector = '.product-card, .order-card') {
  if (!container) return;
  const items = container.querySelectorAll(selector);
  items.forEach((el, i) => {
    el.classList.add('stagger-in', 'ui-enhanced');
    el.style.animationDelay = `${Math.min(i * 70, 420)}ms`;
  });
}

function initPageUI() {
  document.body.classList.add('ui-ready', 'min-h-screen', 'antialiased');

  document.querySelectorAll('.navbar').forEach(nav => {
    nav.classList.add(
      'ui-enhanced',
      'backdrop-blur-md',
      'bg-atelier-bg/90',
      'border-atelier-border',
      'transition-shadow',
      'duration-300'
    );
    const onScroll = () => nav.classList.toggle('scrolled', window.scrollY > 8);
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
  });

  document.querySelectorAll('.brand').forEach(b => {
    b.classList.add(
      'font-display',
      'tracking-widest',
      'transition-colors',
      'duration-200',
      'hover:text-amber-300',
      'no-underline',
      'hover:no-underline'
    );
  });

  document.querySelectorAll('.nav-links a').forEach(a => {
    a.classList.add(
      'transition-colors',
      'duration-200',
      'hover:text-atelier-text',
      'no-underline',
      'hover:no-underline'
    );
  });

  document.querySelectorAll('.container, .admin-wrap').forEach(c => {
    c.classList.add('page-content');
  });

  document.querySelectorAll('.btn').forEach(b => b.classList.add('ui-enhanced'));
  document.querySelectorAll('.cart-summary, .auth-card').forEach(c => c.classList.add('ui-enhanced', 'shadow-card'));
  document.querySelectorAll('.hero').forEach(h => {
    h.classList.add('relative', 'overflow-hidden', 'animate-fade-in');
  });
  document.querySelectorAll('.page-title').forEach(t => {
    t.classList.add('font-display', 'text-atelier-gold', 'animate-slide-up');
  });
  document.querySelectorAll('.loading').forEach(l => l.classList.add('ui-shimmer'));
}

document.addEventListener('DOMContentLoaded', initPageUI);

window.showToast = showToast;
window.setCardMessage = setCardMessage;
window.staggerReveal = staggerReveal;
