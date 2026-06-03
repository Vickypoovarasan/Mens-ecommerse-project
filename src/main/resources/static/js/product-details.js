const params = new URLSearchParams(location.search);
const productId = params.get('id');

let product = null;
let selectedSize = null;
let selectedColor = null;
let selectedVariant = null;
let selectedReviewStars = 5;

async function loadProduct() {
  const main = document.getElementById('detailMain');
  if (!productId) {
    main.innerHTML = '<p class="error-msg">Missing product id.</p>';
    return;
  }

  try {
    product = await apiFetch(`/api/products/${productId}`);
    renderProduct();
  } catch (e) {
    main.innerHTML = `<p class="error-msg">${escapeHtml(e.message)}</p>`;
  }
}

function renderProduct() {
  const p = product;
  const sizes = [...new Set(p.variants.map(v => v.size))];
  const colors = [...new Set(p.variants.map(v => v.color))];

  selectedSize = sizes[0] || null;
  selectedColor = colors[0] || null;
  updateVariant();

  const loggedIn = getSession().loggedIn;
  const stars = '★'.repeat(Math.round(p.averageRating)) + '☆'.repeat(5 - Math.round(p.averageRating));
  const reviewListHtml = p.reviewCount === 0
    ? '<p style="color: var(--muted); margin-bottom: 1rem;">No reviews yet.</p>'
    : p.reviews.map(r => `
            <div class="review-item" style="margin-bottom: 1rem; border-bottom: 1px solid #e5e7eb; padding-bottom: 1rem;">
              <div style="display:flex; justify-content:space-between; align-items:center; gap: 0.75rem; flex-wrap:wrap;">
                <strong>${escapeHtml(r.authorName)}</strong>
                <span>${'★'.repeat(r.stars)}${'☆'.repeat(5 - r.stars)}</span>
              </div>
              <p style="color: var(--muted); margin: 0.35rem 0 0.25rem;">${escapeHtml(r.comment || '')}</p>
              <div style="font-size:0.85rem; color: var(--muted);">${escapeHtml(r.createdAt ? new Date(r.createdAt).toLocaleDateString() : '')}</div>
            </div>
          `).join('');

  const reviewFormHtml = loggedIn
    ? p.canReview
      ? `
            <div class="review-form" style="margin-top: 1.5rem;">
              <h3 style="font-family: var(--font-display); margin-bottom: 0.75rem;">Share your review</h3>
              <div class="star-picker" id="reviewStars" style="margin-bottom: 1rem; font-size: 1.75rem;">
                ${[1,2,3,4,5].map(s => `<button type="button" class="star-btn ${s <= selectedReviewStars ? 'active' : ''}" data-stars="${s}" style="background:none;border:none;cursor:pointer;color:${s <= selectedReviewStars ? '#f59e0b' : '#d1d5db'};font-size:1.75rem;">${s <= selectedReviewStars ? '★' : '☆'}</button>`).join('')}
              </div>
              <textarea id="reviewComment" rows="4" placeholder="Write your comment (optional)" style="width:100%;padding:0.75rem;border:1px solid #cbd5e1;border-radius:0.5rem;resize:vertical;"></textarea>
              <button type="button" class="btn btn-primary" id="submitReviewBtn" style="margin-top:0.75rem;">Submit review</button>
            </div>
          `
      : '<p style="color: var(--muted); margin-top: 1rem;">Reviews are available only after delivery.</p>'
    : '<p style="color: var(--muted); margin-top: 1rem;"><a href="login.html">Sign in</a> to write a review.</p>';

  document.getElementById('detailMain').innerHTML = `
    <div class="detail-layout animate-fade-in">
      <div class="detail-image ${p.imageUrl ? '' : p.imageKey}">
        ${p.imageUrl ? `<img src="${escapeHtml(p.imageUrl)}" alt="${escapeHtml(p.name)}">` : ''}
      </div>
      <div>
        <span class="category">${escapeHtml(p.category)}</span>
        <h1 style="font-family: var(--font-display); font-size: 2.25rem; font-weight: 500; margin: 0.25rem 0;">${escapeHtml(p.name)}</h1>
        <div class="rating">${stars}<span>(${p.reviewCount} reviews)</span></div>
        <p style="color: var(--muted); margin-bottom: 1rem;">${escapeHtml(p.description || '')}</p>
        <p class="detail-price" id="displayPrice">${formatPrice(selectedVariant ? selectedVariant.price : p.basePrice)}</p>

        <div class="variant-section">
          <label>Size</label>
          <div class="size-row" id="sizeRow">
            ${sizes.map(s => `<button type="button" class="size-btn ${s === selectedSize ? 'selected' : ''}" data-size="${s}">${s}</button>`).join('')}
          </div>
          <label>Color</label>
          <div class="color-row" id="colorRow">
            ${colors.map(c => `<button type="button" class="color-swatch ${c === selectedColor ? 'selected' : ''}" data-color="${c}" title="${c}" style="background:${colorCss(c)}"></button>`).join('')}
          </div>
          <p class="stock-msg" id="stockMsg"></p>
        </div>

        <div class="actions">
          <button type="button" class="btn btn-primary" id="addCartBtn">Add to Cart</button>
          <button type="button" class="btn btn-outline" id="buyNowBtn">Buy Now</button>
        </div>

        <div class="reviews-box">
          <h2 style="font-family: var(--font-display); font-size: 1.5rem; margin-bottom: 1rem;">Customer Reviews</h2>
          ${reviewListHtml}
          ${reviewFormHtml}
        </div>
      </div>
    </div>
`;

  bindVariantButtons();
  bindActions();
  updateStockUi();
}

function bindVariantButtons() {
  document.querySelectorAll('.size-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      selectedSize = btn.dataset.size;
      document.querySelectorAll('.size-btn').forEach(b => b.classList.toggle('selected', b === btn));
      updateVariant();
    });
  });
  document.querySelectorAll('.color-swatch').forEach(btn => {
    btn.addEventListener('click', () => {
      selectedColor = btn.dataset.color;
      document.querySelectorAll('.color-swatch').forEach(b => b.classList.toggle('selected', b === btn));
      updateVariant();
    });
  });
}

function updateVariant() {
  selectedVariant = product.variants.find(v => v.size === selectedSize && v.color === selectedColor) || null;
  const priceEl = document.getElementById('displayPrice');
  if (priceEl && selectedVariant) {
    priceEl.textContent = formatPrice(selectedVariant.price);
  }
  updateStockUi();
}

function updateStockUi() {
  const msg = document.getElementById('stockMsg');
  const addBtn = document.getElementById('addCartBtn');
  const buyBtn = document.getElementById('buyNowBtn');
  if (!msg) return;

  if (!selectedVariant) {
    msg.textContent = 'Select size and color';
    if (addBtn) addBtn.disabled = true;
    if (buyBtn) buyBtn.disabled = true;
    return;
  }

  if (selectedVariant.stock <= 0) {
    msg.textContent = 'Out of stock';
    msg.classList.add('low');
    if (addBtn) addBtn.disabled = true;
    if (buyBtn) buyBtn.disabled = true;
  } else if (selectedVariant.stock <= 3) {
    msg.textContent = `Only ${selectedVariant.stock} left in stock`;
    msg.classList.add('low');
    if (addBtn) addBtn.disabled = false;
    if (buyBtn) buyBtn.disabled = false;
  } else {
    msg.textContent = 'In stock';
    msg.classList.remove('low');
    if (addBtn) addBtn.disabled = false;
    if (buyBtn) buyBtn.disabled = false;
  }
}

function bindActions() {
  document.getElementById('addCartBtn').addEventListener('click', () => {
    if (!selectedVariant) return;
    const cart = getCart();
    const existing = cart.find(i => i.variantId === selectedVariant.id);
    if (existing) {
      existing.quantity += 1;
    } else {
      cart.push({
        productId: product.id,
        variantId: selectedVariant.id,
        name: product.name,
        size: selectedVariant.size,
        color: selectedVariant.color,
        price: selectedVariant.price,
        quantity: 1
      });
    }
    saveCart(cart);
    showToast('Added to cart', 'success');
    setTimeout(() => { location.href = 'cart.html'; }, 450);
  });

  document.getElementById('buyNowBtn').addEventListener('click', () => {
    if (!selectedVariant) return;
    const item = {
      productId: product.id,
      variantId: selectedVariant.id,
      name: product.name,
      size: selectedVariant.size,
      color: selectedVariant.color,
      price: selectedVariant.price,
      quantity: 1
    };
    sessionStorage.setItem('buyNowItem', JSON.stringify(item));
    location.href = 'checkout.html';
  });
}

function colorCss(name) {
  const map = {
    Navy: '#1e3a5f', Charcoal: '#36454f', Black: '#111',
    White: '#f5f5f0', Blue: '#3d5a80', Ivory: '#fffff0'
  };
  return map[name] || '#555';
}

loadProduct();
