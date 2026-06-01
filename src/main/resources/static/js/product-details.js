const params = new URLSearchParams(location.search);
const productId = params.get('id');

let product = null;
let selectedSize = null;
let selectedColor = null;
let selectedVariant = null;

const MOCK_REVIEWS = [
  { author: 'Rahul M.', stars: 5, text: 'Excellent tailoring and fabric quality. Fits perfectly after minor alterations.' },
  { author: 'James K.', stars: 4, text: 'Great value for premium menswear. Delivery was on time.' },
  { author: 'Arun S.', stars: 5, text: 'The navy suit looks sharp. Highly recommend for formal events.' }
];

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

  const stars = '★'.repeat(Math.round(p.averageRating)) + '☆'.repeat(5 - Math.round(p.averageRating));

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
          ${MOCK_REVIEWS.map(r => `
            <div class="review-item">
              <strong>${escapeHtml(r.author)}</strong> ${'★'.repeat(r.stars)}
              <p style="color: var(--muted); margin-top: 0.35rem;">${escapeHtml(r.text)}</p>
            </div>
          `).join('')}
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
