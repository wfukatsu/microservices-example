# ScalarDB マイクロサービス API 仕様書

## 📋 API 概要

ScalarDB OSS を使用したマイクロサービスアーキテクチャにおける REST API の詳細仕様です。

### ベースURL
- **APIテスター経由**: `http://localhost:3000`
- **直接アクセス**: 各サービス個別ポート

### 認証
現在の実装では認証は不要です。本番環境では JWT 認証の実装を推奨します。

### レスポンス形式
全APIは JSON 形式でレスポンスを返します。

---

## 🏪 Inventory Service (在庫管理)

### ベースURL: `http://localhost:8081`

#### 1. ヘルスチェック
```http
GET /actuator/health
```

**レスポンス例:**
```json
{
  "status": "UP",
  "service": "inventory-service"
}
```

#### 2. 在庫一覧取得
```http
GET /api/inventory
```

**クエリパラメータ:**
- `category` (optional): カテゴリフィルター

**レスポンス例:**
```json
[
  {
    "productId": "ITEM001",
    "productName": "ノートパソコン",
    "category": "Electronics",
    "quantity": 50,
    "reservedQuantity": 5,
    "availableQuantity": 45,
    "unitPrice": 80000,
    "lowStockThreshold": 10,
    "lastUpdated": "2025-06-18T10:18:48.023Z"
  }
]
```

#### 3. 商品詳細取得
```http
GET /api/inventory/{productId}
```

**パスパラメータ:**
- `productId`: 商品ID

**レスポンス例:**
```json
{
  "productId": "ITEM001",
  "productName": "ノートパソコン",
  "category": "Electronics",
  "quantity": 50,
  "reservedQuantity": 5,
  "availableQuantity": 45,
  "unitPrice": 80000,
  "lowStockThreshold": 10,
  "lastUpdated": "2025-06-18T10:18:48.023Z"
}
```

**エラーレスポンス (404):**
```json
{
  "error": "NOT_FOUND",
  "message": "Product not found"
}
```

#### 4. 在庫更新
```http
PUT /api/inventory/{productId}
```

**リクエストボディ:**
```json
{
  "quantity": 100,
  "reservedQuantity": 10
}
```

**レスポンス例:**
```json
{
  "productId": "ITEM001",
  "productName": "ノートパソコン",
  "category": "Electronics",
  "quantity": 100,
  "reservedQuantity": 10,
  "availableQuantity": 90,
  "unitPrice": 80000,
  "lowStockThreshold": 10,
  "lastUpdated": "2025-06-18T10:20:00.000Z"
}
```

#### 5. 在庫予約
```http
POST /api/inventory/reserve
```

**リクエストボディ:**
```json
{
  "orderId": "ORDER-001",
  "customerId": "CUST-001",
  "items": [
    {
      "productId": "ITEM001",
      "quantity": 2
    }
  ]
}
```

**レスポンス例:**
```json
{
  "reservationId": "uuid",
  "orderId": "ORDER-001",
  "customerId": "CUST-001",
  "items": [
    {
      "productId": "ITEM001",
      "productName": "ノートパソコン",
      "reservedQuantity": 2,
      "unitPrice": 80000
    }
  ],
  "status": "RESERVED",
  "expiresAt": "2025-06-19T10:18:48.023Z",
  "createdAt": "2025-06-18T10:18:48.023Z"
}
```

#### 6. 予約確定
```http
PUT /api/inventory/reservation/{reservationId}/confirm
```

**レスポンス例:**
```json
{
  "reservationId": "uuid",
  "status": "CONFIRMED",
  "confirmedAt": "2025-06-18T10:20:00.000Z"
}
```

#### 7. 予約キャンセル
```http
DELETE /api/inventory/reservation/{reservationId}
```

**レスポンス例:**
```json
{
  "message": "Reservation cancelled"
}
```

#### 8. 在庫補充 (デモ機能)
```http
POST /api/inventory/{productId}/restock
```

**リクエストボディ:**
```json
{
  "quantity": 50,
  "supplier": "SUPPLIER_A",
  "cost": 45000
}
```

**レスポンス例:**
```json
{
  "restockId": "uuid",
  "productId": "ITEM001",
  "productName": "ノートパソコン",
  "quantity": 50,
  "supplier": "SUPPLIER_A",
  "cost": 45000,
  "newTotalQuantity": 100,
  "processedAt": "2025-06-18T10:20:00.000Z"
}
```

---

## 💰 Payment Service (決済管理)

### ベースURL: `http://localhost:8082`

#### 1. ヘルスチェック
```http
GET /actuator/health
```

#### 2. 決済処理
```http
POST /api/payments/process
```

**リクエストボディ:**
```json
{
  "orderId": "ORDER-001",
  "customerId": "CUST-001",
  "amount": 160000,
  "paymentMethod": "CREDIT_CARD",
  "currency": "JPY"
}
```

**レスポンス例 (成功):**
```json
{
  "paymentId": "uuid",
  "orderId": "ORDER-001",
  "customerId": "CUST-001",
  "amount": 160000,
  "currency": "JPY",
  "paymentMethod": "CREDIT_CARD",
  "status": "COMPLETED",
  "transactionId": "TXN_1750242000000",
  "processedAt": "2025-06-18T10:20:00.000Z",
  "providerResponse": {
    "code": "200",
    "message": "Payment processed successfully"
  }
}
```

**レスポンス例 (失敗):**
```json
{
  "error": "PAYMENT_FAILED",
  "message": "Payment processing failed",
  "payment": {
    "paymentId": "uuid",
    "status": "FAILED"
  }
}
```

#### 3. 返金処理
```http
POST /api/payments/{paymentId}/refund
```

**リクエストボディ:**
```json
{
  "amount": 50000,
  "reason": "Customer request"
}
```

**レスポンス例:**
```json
{
  "refundId": "uuid",
  "paymentId": "original-payment-id",
  "amount": 50000,
  "reason": "Customer request",
  "status": "COMPLETED",
  "processedAt": "2025-06-18T10:20:00.000Z",
  "transactionId": "REF_1750242000000"
}
```

#### 4. 顧客決済履歴
```http
GET /api/payments/customer/{customerId}
```

**レスポンス例:**
```json
[
  {
    "paymentId": "uuid",
    "orderId": "ORDER-001",
    "amount": 160000,
    "paymentMethod": "CREDIT_CARD",
    "status": "COMPLETED",
    "processedAt": "2025-06-18T10:20:00.000Z"
  }
]
```

#### 5. ウォレット残高確認 (デモ機能)
```http
GET /api/wallet/{customerId}
```

**レスポンス例:**
```json
{
  "customerId": "DEMO-USER-001",
  "balance": 500000,
  "currency": "JPY",
  "lastUpdated": "2025-06-18T10:20:00.000Z"
}
```

#### 6. ウォレット資金追加 (デモ機能)
```http
POST /api/wallet/{customerId}/add-funds
```

**リクエストボディ:**
```json
{
  "amount": 10000,
  "source": "BANK_TRANSFER"
}
```

**レスポンス例:**
```json
{
  "customerId": "DEMO-USER-001",
  "balance": 510000,
  "currency": "JPY",
  "addedAmount": 10000,
  "source": "BANK_TRANSFER",
  "transactionId": "FUND_1750242000000",
  "processedAt": "2025-06-18T10:20:00.000Z"
}
```

#### 7. ウォレット決済 (デモ機能)
```http
POST /api/payments/process-with-wallet
```

**リクエストボディ:**
```json
{
  "orderId": "ORDER-001",
  "customerId": "DEMO-USER-001",
  "amount": 80000,
  "currency": "JPY"
}
```

**レスポンス例:**
```json
{
  "paymentId": "uuid",
  "orderId": "ORDER-001",
  "customerId": "DEMO-USER-001",
  "amount": 80000,
  "currency": "JPY",
  "paymentMethod": "WALLET",
  "status": "COMPLETED",
  "transactionId": "TXN_1750242000000",
  "processedAt": "2025-06-18T10:20:00.000Z",
  "walletBalanceAfter": 420000,
  "providerResponse": {
    "code": "200",
    "message": "Payment processed from wallet successfully"
  }
}
```

---

## 🚚 Shipping Service (配送管理)

### ベースURL: `http://localhost:8083`

#### 1. ヘルスチェック
```http
GET /actuator/health
```

#### 2. 配送作成
```http
POST /api/shipments/create
```

**リクエストボディ:**
```json
{
  "orderId": "ORDER-001",
  "customerId": "CUST-001",
  "shippingAddress": {
    "name": "田中太郎",
    "postalCode": "100-0001",
    "prefecture": "東京都",
    "city": "千代田区",
    "address": "千代田1-1-1",
    "phone": "03-1234-5678"
  },
  "items": [
    {
      "productId": "ITEM001",
      "quantity": 1,
      "weight": 2.5
    }
  ]
}
```

**レスポンス例:**
```json
{
  "shipmentId": "uuid",
  "orderId": "ORDER-001",
  "customerId": "CUST-001",
  "shippingAddress": {
    "name": "田中太郎",
    "postalCode": "100-0001",
    "prefecture": "東京都",
    "city": "千代田区",
    "address": "千代田1-1-1",
    "phone": "03-1234-5678"
  },
  "items": [
    {
      "productId": "ITEM001",
      "productName": "ノートパソコン",
      "quantity": 1,
      "weight": 2.5
    }
  ],
  "status": "PENDING",
  "trackingNumber": "TRK1750242000000",
  "carrier": "ヤマト運輸",
  "estimatedDelivery": "2025-06-21T10:20:00.000Z",
  "createdAt": "2025-06-18T10:20:00.000Z"
}
```

#### 3. 配送状況更新
```http
PUT /api/shipments/{shipmentId}/status
```

**リクエストボディ:**
```json
{
  "status": "SHIPPED"
}
```

**有効なステータス:**
- `PENDING`: 準備中
- `PROCESSING`: 処理中
- `SHIPPED`: 発送済み
- `DELIVERED`: 配送完了
- `CANCELLED`: キャンセル

**レスポンス例:**
```json
{
  "shipmentId": "uuid",
  "status": "SHIPPED",
  "shippedAt": "2025-06-18T12:00:00.000Z",
  "lastUpdated": "2025-06-18T12:00:00.000Z"
}
```

#### 4. 配送詳細取得
```http
GET /api/shipments/{shipmentId}
```

**レスポンス例:**
```json
{
  "shipmentId": "uuid",
  "orderId": "ORDER-001",
  "status": "SHIPPED",
  "trackingNumber": "TRK1750242000000",
  "carrier": "ヤマト運輸",
  "estimatedDelivery": "2025-06-21T10:20:00.000Z",
  "shippedAt": "2025-06-18T12:00:00.000Z"
}
```

#### 5. 配送追跡 (デモ機能)
```http
GET /api/shipments/track/{trackingNumber}
```

**レスポンス例:**
```json
{
  "trackingNumber": "DEMO1750242000000",
  "currentStatus": "SHIPPED",
  "estimatedDelivery": "2025-06-21T10:20:00.000Z",
  "carrier": "ヤマト運輸",
  "trackingHistory": [
    {
      "status": "PENDING",
      "description": "注文を受け付けました",
      "timestamp": "2025-06-18T10:20:00.000Z",
      "location": "配送センター"
    },
    {
      "status": "PROCESSING",
      "description": "商品を準備中です",
      "timestamp": "2025-06-18T11:20:00.000Z",
      "location": "配送センター"
    },
    {
      "status": "SHIPPED",
      "description": "商品が発送されました",
      "timestamp": "2025-06-18T12:20:00.000Z",
      "location": "配送センター"
    }
  ]
}
```

---

## 🛒 Order Service (注文管理)

### ベースURL: `http://localhost:8080`

#### 1. ヘルスチェック
```http
GET /actuator/health
```

#### 2. 注文処理
```http
POST /api/orders/process
```

**リクエストボディ:**
```json
{
  "customerId": "CUST-001",
  "items": [
    {
      "productId": "ITEM001",
      "quantity": 1
    }
  ],
  "shippingAddress": {
    "name": "田中太郎",
    "postalCode": "100-0001",
    "prefecture": "東京都",
    "city": "千代田区",
    "address": "千代田1-1-1"
  },
  "paymentMethod": "CREDIT_CARD"
}
```

**レスポンス例:**
```json
{
  "orderId": "uuid",
  "customerId": "CUST-001",
  "items": [
    {
      "productId": "ITEM001",
      "quantity": 1
    }
  ],
  "totalAmount": 80000,
  "shippingAddress": {
    "name": "田中太郎",
    "postalCode": "100-0001",
    "prefecture": "東京都",
    "city": "千代田区",
    "address": "千代田1-1-1"
  },
  "paymentMethod": "CREDIT_CARD",
  "status": "PROCESSING",
  "createdAt": "2025-06-18T10:20:00.000Z",
  "steps": {
    "inventory": {
      "status": "PENDING",
      "message": "Reserving inventory..."
    },
    "payment": {
      "status": "PENDING",
      "message": "Processing payment..."
    },
    "shipping": {
      "status": "PENDING",
      "message": "Creating shipment..."
    }
  }
}
```

#### 3. 注文詳細取得
```http
GET /api/orders/{orderId}
```

**レスポンス例:**
```json
{
  "orderId": "uuid",
  "customerId": "CUST-001",
  "status": "CONFIRMED",
  "totalAmount": 80000,
  "createdAt": "2025-06-18T10:20:00.000Z"
}
```

#### 4. 注文一覧取得
```http
GET /api/orders
```

**クエリパラメータ:**
- `page` (default: 1): ページ番号
- `limit` (default: 10): 1ページあたりの件数

**レスポンス例:**
```json
{
  "orders": [
    {
      "orderId": "uuid",
      "customerId": "CUST-001",
      "status": "CONFIRMED",
      "totalAmount": 80000,
      "createdAt": "2025-06-18T10:20:00.000Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 10,
    "total": 1,
    "totalPages": 1
  }
}
```

#### 5. 顧客注文履歴
```http
GET /api/orders/customer/{customerId}
```

**レスポンス例:**
```json
[
  {
    "orderId": "uuid",
    "customerId": "CUST-001",
    "status": "CONFIRMED",
    "totalAmount": 80000,
    "createdAt": "2025-06-18T10:20:00.000Z"
  }
]
```

#### 6. デモ注文処理 (統合処理)
```http
POST /api/orders/demo-process
```

**リクエストボディ:**
```json
{
  "customerId": "DEMO-USER-001",
  "items": [
    {
      "productId": "ITEM001",
      "quantity": 1
    }
  ],
  "shippingAddress": {
    "name": "田中太郎",
    "postalCode": "100-0001",
    "prefecture": "東京都",
    "city": "千代田区",
    "address": "千代田1-1-1",
    "phone": "03-1234-5678"
  },
  "useWallet": true
}
```

**レスポンス例:**
```json
{
  "orderId": "uuid",
  "customerId": "DEMO-USER-001",
  "items": [
    {
      "productId": "ITEM001",
      "quantity": 1
    }
  ],
  "totalAmount": 80000,
  "shippingAddress": {
    "name": "田中太郎",
    "postalCode": "100-0001",
    "prefecture": "東京都",
    "city": "千代田区",
    "address": "千代田1-1-1",
    "phone": "03-1234-5678"
  },
  "paymentMethod": "WALLET",
  "status": "CONFIRMED",
  "createdAt": "2025-06-18T10:20:00.000Z",
  "payment": {
    "paymentId": "uuid",
    "paymentMethod": "WALLET",
    "status": "COMPLETED",
    "walletBalanceAfter": 420000
  },
  "shipment": {
    "shipmentId": "uuid",
    "trackingNumber": "DEMO1750242000000",
    "status": "PENDING",
    "estimatedDelivery": "2025-06-21T10:20:00.000Z"
  }
}
```

---

## 🚨 エラーレスポンス

### 共通エラー形式

#### 400 Bad Request
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Missing required fields: customerId, items, shippingAddress"
}
```

#### 404 Not Found
```json
{
  "error": "NOT_FOUND",
  "message": "Product not found"
}
```

#### 500 Internal Server Error
```json
{
  "error": "INTERNAL_ERROR",
  "message": "An unexpected error occurred"
}
```

### 業務エラー

#### 在庫不足
```json
{
  "error": "INSUFFICIENT_INVENTORY",
  "message": "Insufficient inventory for product ITEM001. Available: 5, Requested: 10"
}
```

#### 決済失敗
```json
{
  "error": "PAYMENT_FAILED",
  "message": "Payment processing failed"
}
```

#### ウォレット残高不足
```json
{
  "error": "INSUFFICIENT_FUNDS",
  "message": "Insufficient wallet balance. Available: 50000, Required: 80000"
}
```

---

## 📊 レート制限

現在の実装ではレート制限はありませんが、本番環境では以下の制限を推奨します：

- **一般API**: 100 requests/minute
- **決済API**: 10 requests/minute
- **ヘルスチェック**: 制限なし

---

## 🔧 開発者向け情報

### テストデータ

#### デモ顧客
- `DEMO-USER-001`: ウォレット残高 ¥500,000
- `DEMO-USER-002`: ウォレット残高 ¥300,000
- `DEMO-USER-003`: 新規顧客

#### デモ商品
- `ITEM001`: ノートパソコン (¥80,000)
- `ITEM002`: スマートフォン (¥60,000)
- `ITEM003`: マウス (¥2,500)

### 開発ツール
- **Postman Collection**: 利用可能
- **Swagger UI**: 将来実装予定
- **API Documentation**: このドキュメント

---

**最終更新日**: 2025年6月18日  
**APIバージョン**: 1.0.0