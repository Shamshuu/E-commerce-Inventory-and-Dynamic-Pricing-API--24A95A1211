-- Users
INSERT INTO users (email, password, tier) VALUES
  ('alice@example.com', '$2b$10$hash1', 'BRONZE'),
  ('bob@example.com', '$2b$10$hash2', 'SILVER'),
  ('carol@example.com', '$2b$10$hash3', 'GOLD');

-- Categories
INSERT INTO categories (name, slug) VALUES
  ('Electronics', 'electronics'),
  ('Clothing', 'clothing');

-- Products
INSERT INTO products (name, slug, description, base_price, status, category_id) VALUES
  ('Smartphone', 'smartphone', 'Latest model', 699.99, 'ACTIVE', 1),
  ('T-Shirt', 't-shirt', '100% cotton', 19.99, 'ACTIVE', 2);

-- Variants
INSERT INTO variants (sku, title, stock_quantity, reserved_quantity, price_adjustment, product_id) VALUES
  ('SM123-BLK', 'Black', 50, 0, 0, 1),
  ('SM123-WHT', 'White', 30, 0, 0, 1),
  ('TS-M', 'Medium', 100, 0, 0, 2),
  ('TS-L', 'Large', 80, 0, 0, 2);

-- Pricing Rules
INSERT INTO pricing_rules (type, min_quantity, percentage, flat_amount, user_tier, promo_code, target_type, target_id, start_at, end_at, usage_limit, usage_per_user, active) VALUES
  ('SEASONAL', NULL, 10.0, NULL, NULL, NULL, 'PRODUCT', 1, NOW(), NOW() + INTERVAL '30 days', NULL, NULL, TRUE),
  ('BULK', 10, 5.0, NULL, NULL, NULL, 'PRODUCT', 1, NULL, NULL, NULL, NULL, TRUE),
  ('USER_TIER', NULL, NULL, 15.0, 'GOLD', NULL, 'PRODUCT', 1, NULL, NULL, NULL, NULL, TRUE),
  ('PROMO', NULL, NULL, 20.0, NULL, 'PROMO2025', 'PRODUCT', 1, NULL, NULL, NULL, NULL, TRUE);

-- Carts
INSERT INTO carts (user_id, status) VALUES (1, 'ACTIVE'), (2, 'ACTIVE');

-- Cart Items
INSERT INTO cart_items (cart_id, variant_id, quantity, unit_price, discounts, subtotal, snapshot_at) VALUES
  (1, 1, 2, 699.99, '{"seasonal": 70}', 1259.98, NOW()),
  (2, 3, 5, 19.99, '{"bulk": 5}', 94.95, NOW());

-- Reservations
INSERT INTO reservations (variant_id, cart_item_id, quantity, expires_at, released) VALUES
  (1, 1, 2, NOW() + INTERVAL '10 minutes', FALSE),
  (3, 2, 5, NOW() + INTERVAL '10 minutes', FALSE);
