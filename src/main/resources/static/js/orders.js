function orderIdFromUrl() {
  return new URLSearchParams(location.search).get('id');
}

function renderBanner() {
  const el = document.getElementById('orderBanner');
  const raw = sessionStorage.getItem('lastOrder');
  if (!raw) {
    el.innerHTML = '';
    return;
  }
  try {
    const o = JSON.parse(raw);
    el.innerHTML = `
      <div class="order-banner animate-scale-in shadow-card rounded-xl">
        <strong>Order #${o.orderId} placed successfully.</strong>
        <span>Total ${formatPrice(o.total)}${o.discount > 0 ? ` (incl. −${formatPrice(o.discount)} promo)` : ''} · Ships to ${escapeHtml(o.shippingAddress || '—')}</span>
      </div>`;
    sessionStorage.removeItem('lastOrder');
  } catch {
    el.innerHTML = '';
  }
}

async function loadList() {
  const root = document.getElementById('orderRoot');
  try {
    const orders = await apiFetch('/api/orders');
    if (!orders.length) {
      root.innerHTML = `
        <div class="cart-empty">
          <p>No orders yet.</p>
          <a href="index.html" class="btn btn-primary" style="display:inline-block;margin-top:1rem;text-decoration:none;">Start Shopping</a>
        </div>`;
      return;
    }

    root.innerHTML = `
      <div class="order-list">
        ${orders.map(o => `
          <article class="order-card ui-enhanced shadow-card rounded-lg transition-all" data-id="${o.id}">
            <div class="order-card-head">
              <strong>Order #${o.id}</strong>
              <span class="status-pill">${escapeHtml(o.status)}</span>
            </div>
            <p class="order-meta">${formatPrice(o.total)} · ${o.itemCount} item(s) · ${formatOrderDate(o.orderDate)}</p>
            <p class="order-meta">Payment: ${escapeHtml(o.paymentStatus)} · Est. delivery ${o.expectedDeliveryDate || '—'}</p>
            <button type="button" class="btn btn-outline btn-sm view-btn" data-id="${o.id}">View &amp; Track</button>
          </article>
        `).join('')}
      </div>`;

    if (typeof staggerReveal === 'function') staggerReveal(root.querySelector('.order-list'), '.order-card');
    root.querySelectorAll('.view-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        location.href = `orders.html?id=${btn.dataset.id}`;
      });
    });
  } catch (e) {
    root.innerHTML = `<p class="error-msg">${escapeHtml(e.message)}</p>`;
    if (typeof showToast === 'function') showToast(e.message, 'error');
  }
}

async function loadDetail(id) {
  const root = document.getElementById('orderRoot');
  try {
    const o = await apiFetch(`/api/orders/${id}`);
    root.innerHTML = `
      <p><a href="orders.html" class="back-link">← All orders</a></p>
      <div class="order-detail">
        <div class="order-card-head" style="margin-bottom:1rem;">
          <strong style="font-size:1.25rem;">Order #${o.id}</strong>
          <span class="status-pill">${escapeHtml(o.status)}</span>
        </div>
        <p class="order-meta">Placed ${formatOrderDate(o.orderDate)} · Payment ${escapeHtml(o.paymentStatus)} (${escapeHtml(o.paymentMethod)})</p>
        <p class="order-meta"><strong>Ship to:</strong> ${escapeHtml(o.shippingAddress || '—')}</p>
        <p class="order-meta">Expected delivery: ${o.expectedDeliveryDate || '—'}${o.actualDeliveryDate ? ` · Delivered ${o.actualDeliveryDate}` : ''}</p>
        ${o.returnReason ? `<p class="order-meta"><strong>Return reason:</strong> ${escapeHtml(o.returnReason)}</p>` : ''}

        <h2 class="detail-section-title">Tracking</h2>
        <ol class="tracking-stepper">
          ${o.trackingSteps.map(s => `
            <li class="tracking-step ${s.completed ? 'done' : ''} ${s.active ? 'active' : ''}">
              <span class="dot"></span>
              <span class="label">${escapeHtml(s.label)}</span>
            </li>
          `).join('')}
        </ol>

        <h2 class="detail-section-title">Items</h2>
        <table class="cart-table">
          <thead><tr><th>Product</th><th>Variant</th><th>Qty</th><th>Line</th></tr></thead>
          <tbody>
            ${o.items.map(i => `
              <tr>
                <td>${escapeHtml(i.productName)}</td>
                <td>${escapeHtml(i.size)} / ${escapeHtml(i.color)}</td>
                <td>${i.quantity}</td>
                <td>${formatPrice(i.lineTotal)}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>

        <aside class="cart-summary" style="margin-top:1.5rem;position:static;">
          <div class="summary-row"><span>Subtotal</span><span>${formatPrice(o.subtotal)}</span></div>
          ${o.discount > 0 ? `<div class="summary-row summary-discount"><span>Promo (${escapeHtml(o.promoCode || '')})</span><span>−${formatPrice(o.discount)}</span></div>` : ''}
          <div class="summary-row"><span>Tax</span><span>${formatPrice(o.tax)}</span></div>
          <div class="summary-row"><span>Shipping</span><span>${o.shipping === 0 ? 'FREE' : formatPrice(o.shipping)}</span></div>
          <div class="summary-row summary-total"><span>Total</span><span>${formatPrice(o.total)}</span></div>
        </aside>

        <div class="order-actions">
          <p class="rule-hint">${escapeHtml(o.cancelMessage)}</p>
          <button type="button" class="btn btn-outline" id="cancelBtn" ${o.canCancel ? '' : 'disabled'}>Cancel Order</button>
          <p class="rule-hint">${escapeHtml(o.returnMessage)}</p>
          ${o.canReturn ? `
            <label for="returnReason" class="form-label" style="display:block;margin-top:1rem;margin-bottom:0.5rem;font-weight:600;">Reason for return</label>
            <textarea id="returnReason" rows="4" style="width:100%;padding:0.85rem;border:1px solid rgba(148,163,184,0.4);border-radius:0.75rem;background:rgba(15,20,25,0.9);color:var(--text);resize:vertical;margin-bottom:0.75rem;" placeholder="Describe why you're returning this product"></textarea>
          ` : ''}
          <button type="button" class="btn btn-outline" id="returnBtn" ${o.canReturn ? '' : 'disabled'}>Request Return</button>
          <p class="msg" id="actionMsg"></p>
        </div>
      </div>`;

    const msg = document.getElementById('actionMsg');
    document.getElementById('cancelBtn').addEventListener('click', async () => {
      if (!confirm('Cancel this order? Stock will be restored.')) return;
      try {
        await apiFetch(`/api/orders/${id}/cancel`, { method: 'POST' });
        setCardMessage(msg, 'Order cancelled.', 'success');
        showToast('Order cancelled', 'success');
        loadDetail(id);
      } catch (e) {
        setCardMessage(msg, e.message, 'error');
      }
    });

    document.getElementById('returnBtn').addEventListener('click', async () => {
      if (!confirm('Request a return for this order?')) return;
      const reasonEl = document.getElementById('returnReason');
      const reason = reasonEl ? reasonEl.value.trim() : '';
      if (!reason) {
        setCardMessage(msg, 'Please provide a reason for the return.', 'error');
        return;
      }
      try {
        await apiFetch(`/api/orders/${id}/return`, {
          method: 'POST',
          body: JSON.stringify({ reason })
        });
        setCardMessage(msg, 'Return requested.', 'success');
        showToast('Return requested', 'success');
        loadDetail(id);
      } catch (e) {
        setCardMessage(msg, e.message, 'error');
      }
    });
  } catch (e) {
    root.innerHTML = `<p class="error-msg">${escapeHtml(e.message)}</p>`;
    showToast(e.message, 'error');
  }
}

function formatOrderDate(iso) {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
}

async function init() {
  if (!getSession().loggedIn) {
    sessionStorage.setItem('postLoginRedirect', location.pathname + location.search);
    location.href = 'login.html';
    return;
  }

  renderBanner();
  const id = orderIdFromUrl();
  if (id) {
    await loadDetail(id);
  } else {
    await loadList();
  }
}

document.addEventListener('DOMContentLoaded', init);
