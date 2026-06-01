let currentCategory = '';
let currentQuery = '';

async function loadProducts(category = currentCategory, query = currentQuery) {
  const grid = document.getElementById('productGrid');
  grid.innerHTML = '<p class="loading">Loading collection…</p>';

  try {
    const params = new URLSearchParams({ page: '0', size: '12' });
    if (category) params.set('category', category);
    if (query) params.set('q', query);

    const data = await apiFetch(`/api/products?${params}`);
    if (!data.items.length) {
      const hint = query
        ? `No products match "${escapeHtml(query)}".`
        : 'No products in this category.';
      grid.innerHTML = `<p class="loading">${hint}</p>`;
      return;
    }

    grid.innerHTML = data.items.map(p => `
      <article class="product-card ui-enhanced shadow-card hover:shadow-glow" onclick="location.href='product-details.html?id=${p.id}'">
        <div class="img-wrap ${p.imageUrl ? '' : p.imageKey}">
          ${p.imageUrl ? `<img src="${escapeHtml(p.imageUrl)}" alt="${escapeHtml(p.name)}">` : ''}
        </div>
        <div class="info">
          <span class="category">${escapeHtml(p.category || '')}</span>
          <h3>${escapeHtml(p.name)}</h3>
          <p class="price">${formatPrice(p.basePrice)}</p>
        </div>
      </article>
    `).join('');
    if (typeof staggerReveal === 'function') staggerReveal(grid, '.product-card');
  } catch (e) {
    grid.innerHTML = `<p class="error-msg">${escapeHtml(e.message)}</p>`;
    showToast(e.message, 'error');
  }
}

function runSearch() {
  currentQuery = document.getElementById('searchInput').value.trim();
  loadProducts();
}

document.getElementById('searchBtn').addEventListener('click', runSearch);

document.getElementById('searchInput').addEventListener('keydown', (e) => {
  if (e.key === 'Enter') runSearch();
});

let searchDebounce;
document.getElementById('searchInput').addEventListener('input', () => {
  clearTimeout(searchDebounce);
  searchDebounce = setTimeout(runSearch, 350);
});

document.querySelectorAll('.filter-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    currentCategory = btn.dataset.category || '';
    loadProducts();
  });
});

loadProducts();
