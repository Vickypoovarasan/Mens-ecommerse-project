ALTER TABLE orders
  ADD COLUMN shipping_address VARCHAR(500) NULL AFTER user_id;
