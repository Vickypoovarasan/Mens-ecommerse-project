ALTER TABLE orders
  ADD COLUMN return_reason VARCHAR(500) NULL AFTER actual_delivery_date;
