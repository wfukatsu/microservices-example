#!/bin/bash

# ScalarDB用のメタデータテーブルと実際のテーブルを作成

echo "Creating ScalarDB metadata and tables..."

# SQLiteデータベースに接続してScalarDB用のテーブルを作成
sqlite3 ../shared/sqlite-data/order.db << 'EOF'

-- ScalarDBメタデータテーブル
CREATE TABLE IF NOT EXISTS "scalardb$metadata" (
    "full_table_name" TEXT PRIMARY KEY,
    "metadata" TEXT NOT NULL
);

-- メタデータを挿入
INSERT OR REPLACE INTO "scalardb$metadata" ("full_table_name", "metadata") VALUES 
('order_service.orders', '{"columns":{"order_id":"TEXT","customer_id":"TEXT","status":"TEXT","total_amount":"TEXT","currency":"TEXT","payment_method":"TEXT","shipping_address":"TEXT","notes":"TEXT","created_at":"TEXT","updated_at":"TEXT","inventory_reservation_id":"TEXT","payment_id":"TEXT","shipment_id":"TEXT","tx_id":"TEXT","tx_state":"TEXT","tx_version":"INT","tx_prepared_at":"BIGINT","tx_committed_at":"BIGINT","before_order_id":"TEXT","before_customer_id":"TEXT","before_status":"TEXT","before_total_amount":"TEXT","before_currency":"TEXT","before_payment_method":"TEXT","before_shipping_address":"TEXT","before_notes":"TEXT","before_created_at":"TEXT","before_updated_at":"TEXT","before_inventory_reservation_id":"TEXT","before_payment_id":"TEXT","before_shipment_id":"TEXT","before_tx_id":"TEXT","before_tx_state":"TEXT","before_tx_version":"INT","before_tx_prepared_at":"BIGINT","before_tx_committed_at":"BIGINT"},"partition-key":["order_id"],"clustering-key":[],"secondary-indexes":[],"transaction":true}'),

('order_service.order_items', '{"columns":{"order_id":"TEXT","product_id":"TEXT","product_name":"TEXT","quantity":"INT","unit_price":"TEXT","total_price":"TEXT","sku":"TEXT","notes":"TEXT","created_at":"TEXT","tx_id":"TEXT","tx_state":"TEXT","tx_version":"INT","tx_prepared_at":"BIGINT","tx_committed_at":"BIGINT","before_order_id":"TEXT","before_product_id":"TEXT","before_product_name":"TEXT","before_quantity":"INT","before_unit_price":"TEXT","before_total_price":"TEXT","before_sku":"TEXT","before_notes":"TEXT","before_created_at":"TEXT","before_tx_id":"TEXT","before_tx_state":"TEXT","before_tx_version":"INT","before_tx_prepared_at":"BIGINT","before_tx_committed_at":"BIGINT"},"partition-key":["order_id","product_id"],"clustering-key":[],"secondary-indexes":[],"transaction":true}');

.quit
EOF

echo "ScalarDB setup completed!"

# アプリケーション起動
echo "Starting application..."
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.15/libexec/openjdk.jdk/Contents/Home
export JWT_SECRET="test-secret-key-for-unit-tests-at-least-256-bits-long-abcdefghijklmnopqrstuvwxyz1234567890"
export JWT_EXPIRATION=86400000
export JWT_ISSUER=order-service

java -jar target/order-service-1.0.0.jar --spring.profiles.active=local