let appliedPromo = null;

function getCheckoutLines() {
  const buyNowRaw = sessionStorage.getItem('buyNowItem');
  if (buyNowRaw) {
    try {
      const item = JSON.parse(buyNowRaw);
      if (item && item.variantId) return [item];
    } catch (_) { /* ignore */ }
  }
  return getCart();
}

function toApiItems(lines) {
  return lines.map(i => ({
    variantId: Number(i.variantId),
    quantity: Number(i.quantity)
  }));
}

function renderSummaryTotals(lines) {
  const discount = appliedPromo ? appliedPromo.discountAmount : 0;
  const totals = calcCartTotals(lines, discount);
  const summary = document.getElementById('checkoutSummary');
  if (!summary) return;

  const discountRow = discount > 0
    ? `<div class="summary-row summary-discount"><span>Promo (${escapeHtml(appliedPromo.code)})</span><span>−${formatPrice(discount)}</span></div>`
    : '';

  summary.innerHTML = `
    <h2>Order Summary</h2>
    <div class="summary-row"><span>Subtotal</span><span>${formatPrice(totals.subtotal)}</span></div>
    ${discountRow}
    <div class="summary-row"><span>Tax (12%)</span><span>${formatPrice(totals.tax)}</span></div>
    <div class="summary-row"><span>Shipping</span><span>${totals.shipping === 0 ? 'FREE' : formatPrice(totals.shipping)}</span></div>
    <div class="summary-row summary-total"><span>Total</span><span>${formatPrice(totals.total)}</span></div>`;
}

async function applyPromoCode() {
  const msg = document.getElementById('promoMsg');
  const codeInput = document.getElementById('promoCode');
  const code = codeInput.value.trim();
  if (!code) {
    appliedPromo = null;
    setCardMessage(msg, 'Enter a promo code.', 'error');
    renderSummaryTotals(getCheckoutLines());
    return;
  }

  const lines = getCheckoutLines();
  const { subtotal } = calcCartTotals(lines);

  try {
    const result = await apiFetch('/api/promos/validate', {
      method: 'POST',
      body: JSON.stringify({ code, subtotal })
    });
    if (!result.valid) {
      appliedPromo = null;
      setCardMessage(msg, result.message || 'Invalid promo code.', 'error');
      renderSummaryTotals(lines);
      return;
    }
    appliedPromo = { code: result.code, discountAmount: result.discountAmount };
    setCardMessage(msg, `Promo ${result.code} applied (−${formatPrice(result.discountAmount)}).`, 'success');
    showToast(`Promo ${result.code} applied`, 'success');
    renderSummaryTotals(lines);
  } catch (e) {
    appliedPromo = null;
    setCardMessage(msg, e.message, 'error');
    renderSummaryTotals(lines);
  }
}

async function renderCheckout() {
  if (!getSession().loggedIn) {
    sessionStorage.setItem('postLoginRedirect', 'checkout.html');
    location.href = 'login.html';
    return;
  }

  appliedPromo = null;
  const root = document.getElementById('checkoutRoot');
  const lines = getCheckoutLines();

  if (!lines.length) {
    root.innerHTML = `
      <div class="cart-empty">
        <p>Nothing to checkout.</p>
        <a href="index.html" class="btn btn-primary" style="display:inline-block;margin-top:1rem;text-decoration:none;">Shop now</a>
      </div>`;
    return;
  }

  let profileAddress = '';
  try {
    const me = await apiFetch('/api/me');
    profileAddress = me.address || '';
  } catch (_) { /* ignore */ }

  const totals = calcCartTotals(lines);

  root.innerHTML = `
    <div class="cart-layout">
      <div class="cart-items">
        <table class="cart-table">
          <thead>
            <tr><th>Item</th><th>Variant</th><th>Qty</th><th>Line</th></tr>
          </thead>
          <tbody>
            ${lines.map(i => `
              <tr>
                <td><strong>${escapeHtml(i.name)}</strong></td>
                <td>${escapeHtml(i.size)} / ${escapeHtml(i.color)}</td>
                <td>${i.quantity}</td>
                <td>${formatPrice(i.price * i.quantity)}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
        <label class="field-label">Promo code</label>
        <div class="promo-row">
          <input type="text" id="promoCode" placeholder="e.g. WELCOME10" maxlength="40" autocomplete="off">
          <button type="button" class="btn btn-outline btn-sm" id="applyPromoBtn">Apply</button>
        </div>
        <p class="msg" id="promoMsg" style="margin:0.25rem 0 1rem;"></p>
        <label class="field-label">Ship to address</label>
        <textarea id="shippingAddress" rows="3" placeholder="Street, city, postal code, country">${escapeHtml(profileAddress)}</textarea>
        <label class="chk" style="margin:0.5rem 0 1rem;"><input type="checkbox" id="saveToProfile" checked> Save address to my account</label>
        <label class="field-label">Payment method</label>
        <select id="paymentMethod">
          <option value="CARD">Card (sandbox gateway)</option>
          <option value="COD">Cash on delivery</option>
        </select>
      </div>
      <aside class="cart-summary ui-enhanced shadow-card rounded-xl">
        <div id="checkoutSummary">
          <h2>Order Summary</h2>
          <div class="summary-row"><span>Subtotal</span><span>${formatPrice(totals.subtotal)}</span></div>
          <div class="summary-row"><span>Tax (12%)</span><span>${formatPrice(totals.tax)}</span></div>
          <div class="summary-row"><span>Shipping</span><span>${totals.shipping === 0 ? 'FREE' : formatPrice(totals.shipping)}</span></div>
          <div class="summary-row summary-total"><span>Total</span><span>${formatPrice(totals.total)}</span></div>
        </div>
        <button type="button" class="btn btn-primary" id="placeOrderBtn" style="width:100%;margin-top:1.25rem;">Place Order</button>
        <p class="msg" id="checkoutMsg" style="margin-top:0.75rem;"></p>
      </aside>
    </div>`;

  document.getElementById('applyPromoBtn').addEventListener('click', applyPromoCode);
  document.getElementById('promoCode').addEventListener('keydown', e => {
    if (e.key === 'Enter') {
      e.preventDefault();
      applyPromoCode();
    }
  });
  document.getElementById('placeOrderBtn').addEventListener('click', placeOrder);
}

async function placeOrder() {
  const btn = document.getElementById('placeOrderBtn');
  const msg = document.getElementById('checkoutMsg');
  const lines = getCheckoutLines();
  if (!lines.length) return;

  const shippingAddress = document.getElementById('shippingAddress').value.trim();
  const promoInput = document.getElementById('promoCode').value.trim();
  if (promoInput && (!appliedPromo || appliedPromo.code.toUpperCase() !== promoInput.toUpperCase())) {
    setCardMessage(msg, 'Click Apply to validate your promo code before placing the order.', 'error');
    return;
  }

  btn.disabled = true;
  setCardMessage(msg, 'Placing order…', 'info');

  const idempotencyKey = generateIdempotencyKey();
  const paymentMethod = document.getElementById('paymentMethod').value;
  const body = {
    paymentMethod,
    items: toApiItems(lines),
    shippingAddress: shippingAddress || '',
    saveToProfile: document.getElementById('saveToProfile').checked
  };
  if (appliedPromo) {
    body.promoCode = appliedPromo.code;
  }

  try {
    const data = await apiFetch('/api/orders/checkout', {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },
      body: JSON.stringify(body)
    });

    if (data.paymentPending && data.paymentSessionId) {
      msg.textContent = 'Confirming card payment (sandbox)…';
      await apiFetch('/api/payments/sandbox/confirm', {
        method: 'POST',
        body: JSON.stringify({ sessionId: data.paymentSessionId })
      });
    }

    sessionStorage.removeItem('buyNowItem');
    saveCart([]);
    sessionStorage.setItem('lastOrder', JSON.stringify(data));
    setCardMessage(msg, 'Order placed successfully!', 'success');
    showToast('Order placed successfully!', 'success', { title: 'Checkout complete' });
    setTimeout(() => { location.href = 'orders.html'; }, 1200);
  } catch (e) {
    setCardMessage(msg, e.message, 'error');
    btn.disabled = false;
  }
}

function generateIdempotencyKey() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
    return 'idk-' + Array.from(crypto.getRandomValues(new Uint8Array(16)))
      .map(b => b.toString(16).padStart(2, '0'))
      .join('');
  }
  return 'idk-' + Math.random().toString(36).slice(2) + Date.now().toString(36);
}

document.addEventListener('DOMContentLoaded', renderCheckout);
