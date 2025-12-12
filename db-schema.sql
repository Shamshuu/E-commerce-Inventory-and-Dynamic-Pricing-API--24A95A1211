-- PostgreSQL schema for E-Commerce Inventory & Dynamic Pricing API
-- Users
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    tier VARCHAR(10) NOT NULL CHECK (tier IN ('BRONZE', 'SILVER', 'GOLD'))
);

-- Categories (hierarchical)
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    parent_id INTEGER REFERENCES categories(id) ON DELETE SET NULL
);

-- Products

CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    base_price DECIMAL(12,2) NOT NULL,
    status VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    category_id INTEGER REFERENCES categories(id)
);

-- Variants
CREATE TABLE variants (
    id SERIAL PRIMARY KEY,
    sku VARCHAR(64) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    stock_quantity INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    price_adjustment DECIMAL(12,2) DEFAULT 0,
    product_id INTEGER REFERENCES products(id)
);

-- Pricing Rules
CREATE TABLE pricing_rules (
    id SERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    min_quantity INTEGER,
    percentage DECIMAL(5,2),
    flat_amount DECIMAL(12,2),
    user_tier VARCHAR(10),
    promo_code VARCHAR(64),
    target_type VARCHAR(10) NOT NULL CHECK (target_type IN ('PRODUCT', 'CATEGORY', 'VARIANT')),
    target_id INTEGER,
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    usage_limit INTEGER,
    usage_per_user INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Carts
CREATE TABLE carts (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    status VARCHAR(15) NOT NULL CHECK (status IN ('ACTIVE', 'CHECKED_OUT', 'EXPIRED'))
);

-- Cart Items
CREATE TABLE cart_items (
    id SERIAL PRIMARY KEY,
    cart_id INTEGER REFERENCES carts(id),
    variant_id INTEGER REFERENCES variants(id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    discounts JSONB,
    subtotal DECIMAL(12,2) NOT NULL,
    snapshot_at TIMESTAMP NOT NULL
);

-- Reservations
CREATE TABLE reservations (
    id SERIAL PRIMARY KEY,
    variant_id INTEGER REFERENCES variants(id),
    cart_item_id INTEGER REFERENCES cart_items(id),
    quantity INTEGER NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    released BOOLEAN NOT NULL DEFAULT FALSE
);

-- Orders
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    cart_id INTEGER REFERENCES carts(id),
    total DECIMAL(12,2) NOT NULL
);

-- Indexes for performance
CREATE INDEX idx_variant_stock ON variants(stock_quantity, reserved_quantity);
CREATE INDEX idx_reservation_expiry ON reservations(expires_at, released);
