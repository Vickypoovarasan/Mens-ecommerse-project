-- Sample menswear catalog for local development

INSERT INTO products (name, description, category, base_price, active) VALUES
('Classic Navy Suit', 'Premium wool blend two-piece suit. Tailored fit for formal occasions.', 'Suits', 299.99, TRUE),
('Charcoal Slim Suit', 'Modern slim-cut suit with notch lapel. Ideal for business and events.', 'Suits', 349.99, TRUE),
('Midnight Black Tuxedo', 'Elegant tuxedo with satin lapels. Perfect for black-tie events.', 'Suits', 449.99, TRUE),
('White Formal Shirt', 'Crisp cotton formal shirt with spread collar.', 'Shirts', 59.99, TRUE),
('Blue Oxford Shirt', 'Versatile oxford cotton shirt for office or smart casual.', 'Shirts', 49.99, TRUE),
('French Cuff Dress Shirt', 'Luxury poplin shirt with French cuffs for cufflinks.', 'Shirts', 79.99, TRUE);

INSERT INTO product_variants (product_id, size, color, sku, price, stock, active) VALUES
(1, 'S', 'Navy', 'SUIT-NAVY-S', 299.99, 12, TRUE),
(1, 'M', 'Navy', 'SUIT-NAVY-M', 299.99, 18, TRUE),
(1, 'L', 'Navy', 'SUIT-NAVY-L', 299.99, 15, TRUE),
(1, 'XL', 'Navy', 'SUIT-NAVY-XL', 309.99, 10, TRUE),
(1, 'XXL', 'Navy', 'SUIT-NAVY-XXL', 319.99, 6, TRUE),

(2, 'S', 'Charcoal', 'SUIT-CHAR-S', 349.99, 8, TRUE),
(2, 'M', 'Charcoal', 'SUIT-CHAR-M', 349.99, 14, TRUE),
(2, 'L', 'Charcoal', 'SUIT-CHAR-L', 349.99, 12, TRUE),
(2, 'XL', 'Charcoal', 'SUIT-CHAR-XL', 359.99, 9, TRUE),
(2, 'XXL', 'Charcoal', 'SUIT-CHAR-XXL', 369.99, 5, TRUE),

(3, 'S', 'Black', 'TUX-BLK-S', 449.99, 5, TRUE),
(3, 'M', 'Black', 'TUX-BLK-M', 449.99, 8, TRUE),
(3, 'L', 'Black', 'TUX-BLK-L', 449.99, 7, TRUE),
(3, 'XL', 'Black', 'TUX-BLK-XL', 459.99, 4, TRUE),
(3, 'XXL', 'Black', 'TUX-BLK-XXL', 469.99, 3, TRUE),

(4, 'S', 'White', 'SHIRT-WHT-S', 59.99, 25, TRUE),
(4, 'M', 'White', 'SHIRT-WHT-M', 59.99, 30, TRUE),
(4, 'L', 'White', 'SHIRT-WHT-L', 59.99, 28, TRUE),
(4, 'XL', 'White', 'SHIRT-WHT-XL', 64.99, 20, TRUE),
(4, 'XXL', 'White', 'SHIRT-WHT-XXL', 69.99, 15, TRUE),

(5, 'S', 'Blue', 'SHIRT-BLU-S', 49.99, 22, TRUE),
(5, 'M', 'Blue', 'SHIRT-BLU-M', 49.99, 26, TRUE),
(5, 'L', 'Blue', 'SHIRT-BLU-L', 49.99, 24, TRUE),
(5, 'XL', 'Blue', 'SHIRT-BLU-XL', 54.99, 18, TRUE),
(5, 'XXL', 'Blue', 'SHIRT-BLU-XXL', 59.99, 12, TRUE),

(6, 'S', 'Ivory', 'SHIRT-IVR-S', 79.99, 10, TRUE),
(6, 'M', 'Ivory', 'SHIRT-IVR-M', 79.99, 14, TRUE),
(6, 'L', 'Ivory', 'SHIRT-IVR-L', 79.99, 12, TRUE),
(6, 'XL', 'Ivory', 'SHIRT-IVR-XL', 84.99, 8, TRUE),
(6, 'XXL', 'Ivory', 'SHIRT-IVR-XXL', 89.99, 5, TRUE);
