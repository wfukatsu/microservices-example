# ScalarDB マイクロサービス デモアプリケーション

## 📋 概要

ScalarDB OSS + Spring Boot を使用したマイクロサービスアーキテクチャのデモンストレーション実装です。SQLiteをバックエンドデータベースとして使用し、分散トランザクション管理、在庫管理、決済処理、配送追跡を統合したEコマースシステムを提供します。

## 🏗️ アーキテクチャ

### システム構成図
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  フロントエンド  │    │  APIテスター    │    │   デモアプリ    │
│   (削除済み)    │    │  (Port: 3000)   │    │  (Port: 3000)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                        ┌───────┴───────┐
                        │ Proxy Server  │
                        │ Express.js    │
                        └───────┬───────┘
                                │
        ┌───────────┬───────────┼───────────┬───────────┐
        │           │           │           │           │
  ┌─────▼────┐ ┌───▼────┐ ┌────▼────┐ ┌───▼────┐ ┌────▼────┐
  │ Inventory│ │Payment │ │Shipping │ │ Order  │ │ Wallet  │
  │Service   │ │Service │ │Service  │ │Service │ │Service  │
  │:8081     │ │:8082   │ │:8083    │ │:8080   │ │:8082   │
  └─────┬────┘ └───┬────┘ └────┬────┘ └───┬────┘ └────┬────┘
        │          │           │          │           │
        └──────────┼───────────┼──────────┼───────────┘
                   │           │          │
            ┌──────▼───────────▼──────────▼──────┐
            │         ScalarDB OSS              │
            │      (Embedded Mode)              │
            └──────┬───────────┬──────────┬─────┘
                   │           │          │
            ┌──────▼──┐ ┌──────▼──┐ ┌─────▼───┐
            │SQLite DB│ │SQLite DB│ │SQLite DB│
            │inventory│ │payment  │ │shipping │
            └─────────┘ └─────────┘ └─────────┘
```

### マイクロサービス構成

| サービス | ポート | 責務 | データベース |
|---------|-------|------|------------|
| **Inventory Service** | 8081 | 在庫管理・予約・補充 | inventory.db |
| **Payment Service** | 8082 | 決済処理・ウォレット管理 | payment.db |
| **Shipping Service** | 8083 | 配送管理・追跡 | shipping.db |
| **Order Service** | 8080 | 注文統合・オーケストレーション | order.db |

## 🚀 クイックスタート

### 1. 環境要件
- Node.js 14.0.0 以上
- npm 6.0.0 以上

### 2. システム起動

```bash
# 1. Mock Server (全マイクロサービス) 起動
cd /Users/wfukatsu/playground-claude/microservice-samples/test
node mock-server.js

# 2. API Tester & Demo App Server 起動
cd /Users/wfukatsu/playground-claude/microservice-samples/api-tester
npm start
```

### 3. アクセス方法

- **APIテスター**: http://localhost:3000
- **デモアプリケーション**: http://localhost:3000/demo
- **API仕様書**: http://localhost:3000/api/docs

## 📱 デモアプリケーション機能

### 🏪 在庫管理 (Inventory Management)
- **在庫一覧表示**: リアルタイム在庫状況確認
- **在庫補充**: サプライヤー別補充機能
- **低在庫アラート**: 閾値ベースの在庫警告

### 💰 ウォレット管理 (Wallet Management)
- **残高確認**: 顧客別ウォレット残高表示
- **資金追加**: 銀行振込・クレジットカード・現金対応
- **取引履歴**: ウォレット操作ログ

### 🛒 注文処理 (Order Processing)
- **マルチアイテム注文**: 複数商品の一括注文
- **決済方法選択**: ウォレット・クレジットカード
- **自動在庫確認**: リアルタイム在庫チェック
- **統合処理**: 在庫予約→決済→配送手配の自動化

### 🚚 配送追跡 (Shipping Tracking)
- **追跡番号検索**: 配送状況リアルタイム確認
- **配送履歴**: 詳細な配送ステップ表示
- **配送業者連携**: ヤマト運輸システム統合

## 📊 API 仕様

### Inventory Service (Port: 8081)

#### 基本在庫API
```http
GET    /api/inventory                    # 在庫一覧取得
GET    /api/inventory/:productId         # 商品詳細取得
PUT    /api/inventory/:productId         # 在庫更新
POST   /api/inventory/reserve            # 在庫予約
PUT    /api/inventory/reservation/:id/confirm  # 予約確定
DELETE /api/inventory/reservation/:id    # 予約キャンセル
```

#### 拡張在庫API
```http
POST   /api/inventory/:productId/restock # 在庫補充 (デモ用)
```

**在庫補充リクエスト例:**
```json
{
  "quantity": 50,
  "supplier": "SUPPLIER_A",
  "cost": 45000
}
```

### Payment Service (Port: 8082)

#### 決済API
```http
POST   /api/payments/process            # 決済処理
POST   /api/payments/:id/refund         # 返金処理
GET    /api/payments/customer/:id       # 顧客決済履歴
```

#### ウォレットAPI (デモ用)
```http
GET    /api/wallet/:customerId          # ウォレット残高確認
POST   /api/wallet/:customerId/add-funds # 資金追加
POST   /api/payments/process-with-wallet # ウォレット決済
```

**ウォレット資金追加例:**
```json
{
  "amount": 10000,
  "source": "BANK_TRANSFER"
}
```

### Shipping Service (Port: 8083)

#### 配送API
```http
POST   /api/shipments/create            # 配送作成
PUT    /api/shipments/:id/status        # 配送状況更新
GET    /api/shipments/:id               # 配送詳細取得
```

#### 配送追跡API (デモ用)
```http
GET    /api/shipments/track/:trackingNumber  # 追跡番号での検索
```

### Order Service (Port: 8080)

#### 注文API
```http
POST   /api/orders/process              # 注文処理
GET    /api/orders/:orderId             # 注文詳細取得
GET    /api/orders                      # 注文一覧取得 (ページング対応)
GET    /api/orders/customer/:id         # 顧客注文履歴
```

#### デモ注文API
```http
POST   /api/orders/demo-process         # 統合デモ注文処理
```

**デモ注文処理例:**
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

## 🔧 技術スタック

### バックエンド
- **フレームワーク**: Express.js 4.18.2
- **データベース**: SQLite 3.43.2.2
- **分散トランザクション**: ScalarDB OSS 3.9.0
- **プロキシ**: http-proxy-middleware 2.0.6
- **API**: RESTful Architecture

### フロントエンド
- **言語**: HTML5 + JavaScript ES6+
- **スタイル**: CSS3 (Grid + Flexbox)
- **HTTP Client**: Fetch API
- **UI/UX**: レスポンシブデザイン

### インフラストラクチャ
- **アーキテクチャ**: マイクロサービス
- **通信**: HTTP/REST + JSON
- **ロードバランシング**: Express Proxy
- **ヘルスチェック**: Spring Boot Actuator パターン

## 🗃️ データモデル

### 在庫管理 (Inventory)
```javascript
{
  productId: "ITEM001",
  productName: "ノートパソコン",
  category: "Electronics",
  quantity: 50,
  reservedQuantity: 5,
  availableQuantity: 45,
  unitPrice: 80000,
  lowStockThreshold: 10,
  lastUpdated: "2025-06-18T10:18:48.023Z"
}
```

### ウォレット管理 (Wallet)
```javascript
{
  customerId: "DEMO-USER-001",
  balance: 500000,
  currency: "JPY",
  lastUpdated: "2025-06-18T10:18:48.023Z"
}
```

### 注文管理 (Order)
```javascript
{
  orderId: "uuid",
  customerId: "DEMO-USER-001",
  items: [...],
  totalAmount: 80000,
  shippingAddress: {...},
  paymentMethod: "WALLET",
  status: "CONFIRMED",
  createdAt: "2025-06-18T10:18:48.023Z",
  payment: {...},
  shipment: {...}
}
```

### 配送追跡 (Tracking)
```javascript
{
  trackingNumber: "DEMO1750242061030",
  currentStatus: "SHIPPED",
  estimatedDelivery: "2025-06-21T10:18:48.023Z",
  carrier: "ヤマト運輸",
  trackingHistory: [
    {
      status: "PENDING",
      description: "注文を受け付けました",
      timestamp: "2025-06-18T10:18:48.023Z",
      location: "配送センター"
    }
  ]
}
```

## 🔄 分散トランザクション

### SAGA パターン実装

#### 注文処理フロー
1. **在庫予約** (Inventory Service)
   - 在庫可用性確認
   - 在庫一時予約
   - 失敗時: 即座にエラー返却

2. **決済処理** (Payment Service)
   - ウォレット残高確認 (ウォレット決済時)
   - 決済実行
   - 失敗時: 在庫予約キャンセル

3. **配送手配** (Shipping Service)
   - 配送情報作成
   - 追跡番号生成
   - 失敗時: 決済キャンセル + 在庫解放

4. **注文確定** (Order Service)
   - 最終注文状態更新
   - 統合レスポンス生成

### 補償トランザクション
各ステップの失敗時には自動的に補償処理が実行されます：
- 在庫予約の自動解放
- 決済の自動キャンセル
- 配送手配の自動取り消し

## 🛠️ 開発・運用

### ログ管理
```bash
# Mock Server ログ
tail -f logs/mock-server.log

# プロキシサーバー ログ
# コンソール出力で確認
```

### ヘルスチェック
```bash
# 全サービス状態確認
curl http://localhost:8081/actuator/health  # Inventory
curl http://localhost:8082/actuator/health  # Payment
curl http://localhost:8083/actuator/health  # Shipping  
curl http://localhost:8080/actuator/health  # Order
```

### パフォーマンスモニタリング
- リクエスト処理時間: ログで追跡
- スループット: 30秒間隔でヘルスチェック
- エラー率: レスポンスステータス監視

## 🧪 テスト

### 機能テスト
1. **在庫管理**
   - 在庫一覧取得: `GET /api/inventory`
   - 在庫補充: `POST /api/inventory/ITEM001/restock`

2. **ウォレット操作**
   - 残高確認: `GET /api/wallet/DEMO-USER-001`
   - 資金追加: `POST /api/wallet/DEMO-USER-001/add-funds`

3. **注文処理**
   - 統合注文: `POST /api/orders/demo-process`
   - 注文確認: `GET /api/orders/:orderId`

4. **配送追跡**
   - 追跡確認: `GET /api/shipments/track/:trackingNumber`

### 負荷テスト
```bash
# Apache Bench 使用例
ab -n 100 -c 10 http://localhost:8081/api/inventory
```

## 🚀 本番環境デプロイ

### Docker化 (将来の拡張)
```dockerfile
# 例: Inventory Service
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
EXPOSE 8081
CMD ["node", "inventory-service.js"]
```

### 環境変数設定
```bash
# データベース設定
SCALARDB_STORAGE=jdbc
SCALARDB_CONTACT_POINTS=jdbc:sqlite:shared/sqlite-data/service.db
SCALARDB_ISOLATION_LEVEL=SNAPSHOT

# サービス設定
INVENTORY_PORT=8081
PAYMENT_PORT=8082
SHIPPING_PORT=8083
ORDER_PORT=8080
```

## 📈 拡張性

### 水平スケーリング
- 各マイクロサービスの独立スケーリング
- ロードバランサーによる負荷分散
- ScalarDB OSS クラスター化

### 新機能追加
- 商品カタログサービス
- 顧客管理サービス  
- 通知サービス
- アナリティクスサービス

### 外部システム連携
- 決済ゲートウェイ (Stripe, PayPal)
- 配送業者API (ヤマト運輸, 佐川急便)
- 在庫管理システム (SAP, Oracle)

## 🔒 セキュリティ

### 実装済み対策
- CORS 設定による同一オリジン制限
- 入力値検証によるSQLインジェクション防止
- HTTPSプロキシ対応

### 推奨追加対策
- JWT認証トークン
- Rate Limiting
- API Key管理
- データ暗号化

## 📞 サポート・トラブルシューティング

### よくある問題

#### 1. 504 Gateway Timeout エラー
**原因**: Mock Serverが停止
**解決法**:
```bash
node /path/to/mock-server.js &
```

#### 2. CORS エラー
**原因**: プロキシサーバー設定不備
**解決法**: API Tester経由でアクセス (`http://localhost:3000`)

#### 3. JSON Parse エラー
**原因**: 非JSON レスポンス
**解決法**: Content-Type確認、エラーハンドリング強化済み

### ログファイル
- Mock Server: `logs/mock-server.log`
- API Tester: コンソール出力

### 開発者連絡先
- Issues: プロジェクトGitHubリポジトリ
- Email: development-team@example.com

---

**最終更新日**: 2025年6月18日  
**バージョン**: 1.0.0  
**ライセンス**: MIT