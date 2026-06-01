const NEXT_STATUS = {
  PLACED: { label: 'Confirm', status: 'CONFIRMED' },
  CONFIRMED: { label: 'Mark packed', status: 'PACKED' },
  PACKED: { label: 'Mark shipped', status: 'SHIPPED' },
  SHIPPED: { label: 'Mark delivered', status: 'DELIVERED' }
};

async function requireAdmin() {
  if (!getSession().loggedIn) {
    sessionStorage.setItem('postLoginRedirect', 'admin-dashboard.html');
    location.href = 'login.html';
    return null;
  }
  const me = await apiFetch('/api/me');
  if (me.role !== 'ADMIN') {
    location.href = 'index.html';
    return null;
  }
  return me;
}

function switchTab(tab) {
  document.querySelectorAll('.admin-tabs .filter-btn').forEach(b => {
    b.classList.toggle('active', b.dataset.tab === tab);
  });
  document.querySelectorAll('.admin-panel').forEach(p => p.classList.add('hidden'));
  document.getElementById(`tab-${tab}`).classList.remove('hidden');
}

async function loadDispatch() {
  const root = document.getElementById('dispatchRoot');
  try {
    const orders = await apiFetch('/api/admin/orders/dispatch');
    if (!orders.length) {
      root.innerHTML = '<p class="order-meta">No orders in dispatch queue.</p>';
      return;
    }
    root.innerHTML = orders.map(o => renderOrderRow(o, true)).join('');
    bindOrderActions(root);
  } catch (e) {
    root.innerHTML = `<p class="error-msg">${escapeHtml(e.message)}</p>`;
  }
}

async function loadReturns() {
  const root = document.getElementById('returnsRoot');
  try {
    const orders = await apiFetch('/api/admin/orders/returns');
    if (!orders.length) {
      root.innerHTML = '<p class="order-meta">No return requests.</p>';
      return;
    }
    root.innerHTML = orders.map(o => `
      <article class="order-card admin-order-row" data-id="${o.id}">
        ${renderOrderHead(o)}
        <button type="button" class="btn btn-primary btn-sm approve-return">Approve return</button>
      </article>
    `).join('');
    root.querySelectorAll('.approve-return').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.closest('.admin-order-row').dataset.id;
        if (!confirm('Approve return and restore stock?')) return;
        try {
          await apiFetch(`/api/admin/orders/${id}/returns/approve`, { method: 'POST' });
          loadReturns();
        } catch (err) {
          alert(err.message);
        }
      });
    });
  } catch (e) {
    root.innerHTML = `<p class="error-msg">${escapeHtml(e.message)}</p>`;
  }
}

function renderOrderHead(o) {
  return `
    <div class="order-card-head">
      <strong>Order #${o.id}</strong>
      <span class="status-pill">${escapeHtml(o.status)}</span>
    </div>
    <p class="order-meta">${escapeHtml(o.customerEmail)} · ${formatPrice(o.total)} · ${o.itemCount} item(s)</p>`;
}

function renderCompletedOrderRow(o) {
  return `
    <article class="order-card admin-order-row">
      ${renderOrderHead(o)}
      <p class="order-meta">Ordered: ${escapeHtml(o.orderDate || 'N/A')}</p>
      <p class="order-meta">Delivered: ${escapeHtml(o.actualDeliveryDate || 'N/A')}</p>
    </article>`;
}

async function loadCompletedOrders(from, to) {
  const root = document.getElementById('completedRoot');
  try {
    const params = [];
    if (from) params.push(`from=${encodeURIComponent(from)}`);
    if (to) params.push(`to=${encodeURIComponent(to)}`);
    const url = `/api/admin/orders/completed${params.length ? `?${params.join('&')}` : ''}`;
    const orders = await apiFetch(url);

    root.innerHTML = `
      <div class="admin-product-form" style="margin-bottom:1rem;">
        <div class="form-grid">
          <label style="display:block;">
            From
            <input id="completedFrom" type="date" value="${escapeHtml(from || '')}">
          </label>
          <label style="display:block;">
            To
            <input id="completedTo" type="date" value="${escapeHtml(to || '')}">
          </label>
        </div>
        <div style="margin-top:0.75rem;display:flex;gap:0.5rem;flex-wrap:wrap;">
          <button type="button" class="btn btn-primary btn-sm" id="completedFilterBtn">Filter</button>
          <button type="button" class="btn btn-outline btn-sm" id="completedClearBtn">Clear</button>
        </div>
      </div>
      ${orders.length ? orders.map(renderCompletedOrderRow).join('') : '<p class="order-meta">No completed orders for this range.</p>'}`;

    document.getElementById('completedFilterBtn').addEventListener('click', () => {
      loadCompletedOrders(
        document.getElementById('completedFrom').value,
        document.getElementById('completedTo').value
      );
    });
    document.getElementById('completedClearBtn').addEventListener('click', () => {
      document.getElementById('completedFrom').value = '';
      document.getElementById('completedTo').value = '';
      loadCompletedOrders();
    });
  } catch (e) {
    root.innerHTML = `<p class="error-msg">${escapeHtml(e.message)}</p>`;
  }
}

function renderOrderRow(o, withCancel) {
  const next = NEXT_STATUS[o.status];
  const cancelBtn = withCancel && (o.status === 'PLACED' || o.status === 'CONFIRMED')
    ? `<button type="button" class="btn btn-outline btn-sm cancel-order">Cancel</button>` : '';
  return `
    <article class="order-card admin-order-row" data-id="${o.id}" data-status="${escapeHtml(o.status)}">
      ${renderOrderHead(o)}
      <div class="admin-order-actions">
        ${next ? `<button type="button" class="btn btn-primary btn-sm advance-order" data-next="${next.status}">${next.label}</button>` : ''}
        ${cancelBtn}
      </div>
    </article>`;
}

function bindOrderActions(root) {
  root.querySelectorAll('.advance-order').forEach(btn => {
    btn.addEventListener('click', async () => {
      const row = btn.closest('.admin-order-row');
      const id = row.dataset.id;
      const status = btn.dataset.next;
      try {
        await apiFetch(`/api/admin/orders/${id}/status`, {
          method: 'PATCH',
          body: JSON.stringify({ status })
        });
        loadDispatch();
      } catch (err) {
        alert(err.message);
      }
    });
  });
  root.querySelectorAll('.cancel-order').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.closest('.admin-order-row').dataset.id;
      if (!confirm('Cancel this order?')) return;
      try {
        await apiFetch(`/api/admin/orders/${id}/status`, {
          method: 'PATCH',
          body: JSON.stringify({ status: 'CANCELLED' })
        });
        loadDispatch();
      } catch (err) {
        alert(err.message);
      }
    });
  });
}

async function loadProducts() {
  const root = document.getElementById('productsRoot');
  let editingProductId = null;

  function resetProductForm() {
    editingProductId = null;
    document.getElementById('pName').value = '';
    document.getElementById('pCategory').value = '';
    document.getElementById('pImageUrl').value = '';
    document.getElementById('pPrice').value = '';
    document.getElementById('pDesc').value = '';
    document.getElementById('pActive').checked = true;
    document.getElementById('addProductBtn').textContent = 'Add product';
  }

  try {
    const products = await apiFetch('/api/admin/products');
    root.innerHTML = `
      <div class="admin-product-form">
        <h3 style="font-size:1rem;color:var(--gold);margin-bottom:0.75rem;">Add product</h3>
        <div class="form-grid">
          <input id="pName" placeholder="Name" type="text">
          <input id="pCategory" placeholder="Category (Suits/Shirts)" type="text">
          <input id="pImageUrl" placeholder="Image URL" type="text">
          <input id="pPrice" placeholder="Base price" type="number" step="0.01" min="0">
          <label class="chk"><input id="pActive" type="checkbox" checked> Active</label>
        </div>
        <textarea id="pDesc" placeholder="Description" rows="2" style="width:100%;margin:0.5rem 0;padding:0.5rem;background:var(--bg);border:1px solid var(--border);color:var(--text);font-family:inherit;border-radius:4px;"></textarea>
        <button type="button" class="btn btn-primary btn-sm" id="addProductBtn">Add product</button>
        <p class="msg" id="productFormMsg"></p>
      </div>
      <table class="cart-table" style="margin-top:1.5rem;">
        <thead><tr><th>ID</th><th>Name</th><th>Category</th><th>Price</th><th>Active</th><th></th></tr></thead>
        <tbody>
          ${products.map(p => `
            <tr data-id="${p.id}">
              <td>${p.id}</td>
              <td>${escapeHtml(p.name)}</td>
              <td>${escapeHtml(p.category || '')}</td>
              <td>${formatPrice(p.basePrice)}</td>
              <td>${p.active ? 'Yes' : 'No'}</td>
              <td>
                <button type="button" class="link-btn edit-product">Edit</button>
                <button type="button" class="link-btn variants-product" style="color:var(--gold);margin-left:0.5rem;">Variants</button>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
      <div id="variantPanel" class="variant-panel hidden"></div>`;

    const addButton = document.getElementById('addProductBtn');
    const productFormMsg = document.getElementById('productFormMsg');

    addButton.addEventListener('click', async () => {
      try {
        const payload = {
          name: document.getElementById('pName').value,
          description: document.getElementById('pDesc').value,
          category: document.getElementById('pCategory').value,
          imageUrl: document.getElementById('pImageUrl').value.trim(),
          basePrice: Number(document.getElementById('pPrice').value),
          active: document.getElementById('pActive').checked
        };

        const url = editingProductId ? `/api/admin/products/${editingProductId}` : '/api/admin/products';
        const method = editingProductId ? 'PUT' : 'POST';

        await apiFetch(url, {
          method,
          body: JSON.stringify(payload)
        });

        setCardMessage(productFormMsg, editingProductId ? 'Product updated.' : 'Product added.', 'success');
        showToast(editingProductId ? 'Product updated' : 'Product added', 'success');
        resetProductForm();
        loadProducts();
      } catch (e) {
        setCardMessage(productFormMsg, e.message, 'error');
      }
    });

    root.querySelectorAll('.edit-product').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = btn.closest('tr').dataset.id;
        const p = products.find(x => String(x.id) === id);
        if (!p) return;

        editingProductId = id;
        document.getElementById('pName').value = p.name || '';
        document.getElementById('pCategory').value = p.category || '';
        document.getElementById('pImageUrl').value = p.imageUrl || '';
        document.getElementById('pPrice').value = p.basePrice || '';
        document.getElementById('pDesc').value = p.description || '';
        document.getElementById('pActive').checked = p.active;
        addButton.textContent = 'Update product';
        productFormMsg.textContent = '';
        document.querySelector('.admin-product-form').scrollIntoView({ behavior: 'smooth' });
      });
    });

    root.querySelectorAll('.variants-product').forEach(btn => {
      btn.addEventListener('click', () => {
        openVariantPanel(btn.closest('tr').dataset.id);
      });
    });
  } catch (e) {
    root.innerHTML = `<p class="error-msg">${escapeHtml(e.message)}</p>`;
  }
}

function variantPayloadFromRow(row) {
  return {
    size: row.querySelector('.v-size').value.trim(),
    color: row.querySelector('.v-color').value.trim(),
    sku: row.querySelector('.v-sku').value.trim(),
    price: Number(row.querySelector('.v-price').value),
    stock: Number(row.querySelector('.v-stock').value),
    active: row.querySelector('.v-active').checked
  };
}

async function openVariantPanel(productId) {
  const panel = document.getElementById('variantPanel');
  panel.classList.remove('hidden');
  panel.innerHTML = '<p class="loading">Loading variants…</p>';

  try {
    const variants = await apiFetch(`/api/admin/products/${productId}/variants`);
    panel.innerHTML = `
      <h3 style="margin:0 0 1rem;color:var(--gold);">Variants — product #${productId}</h3>
      <div class="admin-product-form" style="margin-bottom:1rem;">
        <h4 style="font-size:0.9rem;color:var(--muted);margin-bottom:0.5rem;">Add variant</h4>
        <div class="form-grid">
          <input id="nvSize" placeholder="Size (S/M/L)" type="text">
          <input id="nvColor" placeholder="Color" type="text">
          <input id="nvSku" placeholder="SKU" type="text">
          <input id="nvPrice" placeholder="Price" type="number" step="0.01" min="0.01">
          <input id="nvStock" placeholder="Stock" type="number" min="0" value="0">
          <label class="chk"><input id="nvActive" type="checkbox" checked> Active</label>
        </div>
        <button type="button" class="btn btn-primary btn-sm" id="addVariantBtn">Add variant</button>
        <p class="msg" id="variantFormMsg"></p>
      </div>
      ${variants.length ? `
        <table class="cart-table">
          <thead>
            <tr><th>SKU</th><th>Size</th><th>Color</th><th>Price</th><th>Stock</th><th>Active</th><th></th></tr>
          </thead>
          <tbody>
            ${variants.map(v => `
              <tr class="variant-row" data-vid="${v.id}">
                <td><input class="v-sku" value="${escapeHtml(v.sku)}" style="width:6rem;padding:0.25rem;background:var(--bg);border:1px solid var(--border);color:var(--text);"></td>
                <td><input class="v-size" value="${escapeHtml(v.size)}" style="width:3rem;padding:0.25rem;background:var(--bg);border:1px solid var(--border);color:var(--text);"></td>
                <td><input class="v-color" value="${escapeHtml(v.color)}" style="width:5rem;padding:0.25rem;background:var(--bg);border:1px solid var(--border);color:var(--text);"></td>
                <td><input class="v-price" type="number" step="0.01" value="${v.price}" style="width:5rem;padding:0.25rem;background:var(--bg);border:1px solid var(--border);color:var(--text);"></td>
                <td><input class="v-stock" type="number" min="0" value="${v.stock}" style="width:4rem;padding:0.25rem;background:var(--bg);border:1px solid var(--border);color:var(--text);"></td>
                <td><input class="v-active" type="checkbox" ${v.active ? 'checked' : ''}></td>
                <td><button type="button" class="btn btn-outline btn-sm save-variant">Save</button></td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      ` : '<p class="order-meta">No variants yet. Add one above.</p>'}`;

    document.getElementById('addVariantBtn').addEventListener('click', async () => {
      const msg = document.getElementById('variantFormMsg');
      try {
        await apiFetch(`/api/admin/products/${productId}/variants`, {
          method: 'POST',
          body: JSON.stringify({
            size: document.getElementById('nvSize').value,
            color: document.getElementById('nvColor').value,
            sku: document.getElementById('nvSku').value,
            price: Number(document.getElementById('nvPrice').value),
            stock: Number(document.getElementById('nvStock').value),
            active: document.getElementById('nvActive').checked
          })
        });
        setCardMessage(msg, 'Variant added.', 'success');
        showToast('Variant added', 'success');
        openVariantPanel(productId);
      } catch (e) {
        setCardMessage(msg, e.message, 'error');
      }
    });

    panel.querySelectorAll('.save-variant').forEach(btn => {
      btn.addEventListener('click', async () => {
        const row = btn.closest('.variant-row');
        const vid = row.dataset.vid;
        try {
          await apiFetch(`/api/admin/variants/${vid}`, {
            method: 'PUT',
            body: JSON.stringify(variantPayloadFromRow(row))
          });
          btn.textContent = 'Saved';
        } catch (e) {
          alert(e.message);
        }
      });
    });
  } catch (e) {
    panel.innerHTML = `<p class="error-msg">${escapeHtml(e.message)}</p>`;
  }
}

document.querySelectorAll('.admin-tabs .filter-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    switchTab(btn.dataset.tab);
    if (btn.dataset.tab === 'dispatch') loadDispatch();
    if (btn.dataset.tab === 'returns') loadReturns();
    if (btn.dataset.tab === 'completed') loadCompletedOrders();
    if (btn.dataset.tab === 'products') loadProducts();
  });
});

document.addEventListener('DOMContentLoaded', async () => {
  const me = await requireAdmin();
  if (!me) return;
  updateNavbar();
  loadDispatch();
});
