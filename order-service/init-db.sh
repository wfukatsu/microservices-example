#!/bin/bash

# Create ScalarDB tables using Java application classpath
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.15/libexec/openjdk.jdk/Contents/Home

# Set up environment
export JWT_SECRET="test-secret-key-for-unit-tests-at-least-256-bits-long-abcdefghijklmnopqrstuvwxyz1234567890"
export JWT_EXPIRATION=86400000
export JWT_ISSUER=order-service

# Create SQLite database and tables using direct SQL
echo "Creating SQLite database and tables..."

# Create the database file
touch ../shared/sqlite-data/order.db

# Create tables using SQLite
sqlite3 ../shared/sqlite-data/order.db << 'EOF'
-- Create orders table
CREATE TABLE IF NOT EXISTS order_service_orders (
    order_id TEXT PRIMARY KEY,
    customer_id TEXT NOT NULL,
    status TEXT NOT NULL,
    total_amount TEXT NOT NULL,
    currency TEXT DEFAULT 'JPY',
    payment_method TEXT,
    shipping_address TEXT,
    notes TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    inventory_reservation_id TEXT,
    payment_id TEXT,
    shipment_id TEXT,
    tx_id TEXT,
    tx_state TEXT,
    tx_version INTEGER DEFAULT 1,
    tx_prepared_at BIGINT,
    tx_committed_at BIGINT,
    before_order_id TEXT,
    before_customer_id TEXT,
    before_status TEXT,
    before_total_amount TEXT,
    before_currency TEXT,
    before_payment_method TEXT,
    before_shipping_address TEXT,
    before_notes TEXT,
    before_created_at TEXT,
    before_updated_at TEXT,
    before_inventory_reservation_id TEXT,
    before_payment_id TEXT,
    before_shipment_id TEXT,
    before_tx_id TEXT,
    before_tx_state TEXT,
    before_tx_version INTEGER,
    before_tx_prepared_at BIGINT,
    before_tx_committed_at BIGINT
);

-- Create orders_by_customer table
CREATE TABLE IF NOT EXISTS order_service_orders_by_customer (
    customer_id TEXT,
    created_at TEXT,
    order_id TEXT,
    status TEXT,
    total_amount TEXT,
    tx_id TEXT,
    tx_state TEXT,
    tx_version INTEGER DEFAULT 1,
    tx_prepared_at BIGINT,
    tx_committed_at BIGINT,
    before_customer_id TEXT,
    before_created_at TEXT,
    before_order_id TEXT,
    before_status TEXT,
    before_total_amount TEXT,
    before_tx_id TEXT,
    before_tx_state TEXT,
    before_tx_version INTEGER,
    before_tx_prepared_at BIGINT,
    before_tx_committed_at BIGINT,
    PRIMARY KEY (customer_id, created_at)
);

-- Create order_items table
CREATE TABLE IF NOT EXISTS order_service_order_items (
    order_id TEXT,
    product_id TEXT,
    product_name TEXT,
    quantity INTEGER,
    unit_price TEXT,
    total_price TEXT,
    sku TEXT,
    notes TEXT,
    created_at TEXT,
    tx_id TEXT,
    tx_state TEXT,
    tx_version INTEGER DEFAULT 1,
    tx_prepared_at BIGINT,
    tx_committed_at BIGINT,
    before_order_id TEXT,
    before_product_id TEXT,
    before_product_name TEXT,
    before_quantity INTEGER,
    before_unit_price TEXT,
    before_total_price TEXT,
    before_sku TEXT,
    before_notes TEXT,
    before_created_at TEXT,
    before_tx_id TEXT,
    before_tx_state TEXT,
    before_tx_version INTEGER,
    before_tx_prepared_at BIGINT,
    before_tx_committed_at BIGINT,
    PRIMARY KEY (order_id, product_id)
);

.quit
EOF

echo "Database initialized successfully!"

# Start the application
echo "Starting the application..."
java -jar target/order-service-1.0.0.jar --spring.profiles.active=local