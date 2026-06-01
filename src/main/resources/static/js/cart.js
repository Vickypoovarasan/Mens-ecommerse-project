function renderCart() {
  const root = document.getElementById('cartRoot');
  const cart = getCart();

  if (!cart.length) {
    root.innerHTML = `
      <div class="cart-empty">
        <p>Your cart is empty.</p>
        <a href="index.html" class="btn btn-primary" style="display:inline-block;margin-top:1rem;text-decoration:none;">Continue Shopping</a>
      </div>`;
    return;
  }

  const totals = calcCartTotals(cart);
  const shippingNote = totals.subtotal >= FREE_SHIPPING_MIN
    ? 'Free shipping applied'
    : `Add ${formatPrice(FREE_SHIPPING_MIN - totals.subtotal)} more for free shipping`;

  root.innerHTML = `
    <div class="cart-layout">
      <div class="cart-items">
        <table class="cart-table">
          <thead>
            <tr>
              <th>Product</th>
              <th>Variant</th>
              <th>Price</th>
              <th>Qty</th>
              <th>Line total</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            ${cart.map((item, idx) => `
              <tr data-idx="${idx}">
                <td><strong>${escapeHtml(item.name)}</strong></td>
                <td>${escapeHtml(item.size)} / ${escapeHtml(item.color)}</td>
                <td>${formatPrice(item.price)}</td>
                <td>
                  <div class="qty-control">
                    <button type="button" class="qty-btn" data-action="dec" data-idx="${idx}" aria-label="Decrease">−</button>
                    <span class="qty-val">${item.quantity}</span>
                    <button type="button" class="qty-btn" data-action="inc" data-idx="${idx}" aria-label="Increase">+</button>
                  </div>
                </td>
                <td>${formatPrice(item.price * item.quantity)}</td>
                <td><button type="button" class="link-btn" data-action="remove" data-idx="${idx}">Remove</button></td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
      <aside class="cart-summary ui-enhanced shadow-card rounded-xl">
        <h2>Order Summary</h2>
        <div class="summary-row"><span>Subtotal</span><span>${formatPrice(totals.subtotal)}</span></div>
        <div class="summary-row"><span>Tax (12%)</span><span>${formatPrice(totals.tax)}</span></div>
        <div class="summary-row"><span>Shipping</span><span>${totals.shipping === 0 ? 'FREE' : formatPrice(totals.shipping)}</span></div>
        <p class="shipping-hint">${shippingNote}</p>
        <div class="summary-row summary-total"><span>Total</span><span>${formatPrice(totals.total)}</span></div>
        <button type="button" class="btn btn-primary" id="checkoutBtn" style="width:100%;margin-top:1.25rem;">Proceed to Checkout</button>
        <a href="index.html" class="btn btn-outline" style="display:block;text-align:center;margin-top:0.75rem;text-decoration:none;">Continue Shopping</a>
      </aside>
    </div>`;

  root.querySelectorAll('[data-action]').forEach(btn => {
    btn.addEventListener('click', () => {
      const idx = Number(btn.dataset.idx);
      const cart = getCart();
      if (idx < 0 || idx >= cart.length) return;

      if (btn.dataset.action === 'remove') {
        cart.splice(idx, 1);
      } else if (btn.dataset.action === 'inc') {
        cart[idx].quantity += 1;
      } else if (btn.dataset.action === 'dec') {
        if (cart[idx].quantity <= 1) cart.splice(idx, 1);
        else cart[idx].quantity -= 1;
      }

      saveCart(cart);
      renderCart();
    });
  });

  document.getElementById('checkoutBtn').addEventListener('click', () => {
    const { loggedIn } = getSession();
    if (!loggedIn) {
      sessionStorage.setItem('postLoginRedirect', 'checkout.html');
      location.href = 'login.html';
      return;
    }
    location.href = 'checkout.html';
  });
}

document.addEventListener('DOMContentLoaded', renderCart);
