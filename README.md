# ScalarDB Microservices Samples

ScalarDB OSS + Spring Boot で構築されたマイクロサービスアーキテクチャのサンプル実装です。分散トランザクション処理、Process API パターン、包括的な監視・テスト機能を含みます。

## 🏗️ アーキテクチャ概要

### System API (単一ドメインCRUD)
- **Inventory Service** (Port 8081) - 在庫管理API
- **Payment Service** (Port 8082) - 決済処理API  
- **Shipping Service** (Port 8083) - 配送管理API

### Process API (複数System API連携)
- **Order Service** (Port 8080) - 注文プロセスAPI (分散トランザクション)

### 技術スタック
- **Spring Boot 3.2.0** + **Java 17**
- **ScalarDB OSS 3.9.0** (分散トランザクション)
- **SQLite** (開発用データベース)
- **Spring Cloud OpenFeign** (サービス間通信)
- **Resilience4j** (Circuit Breaker, Retry)
- **Micrometer + Prometheus** (メトリクス)
- **WireMock** (テスト用モック)

## 🚀 クイックスタート

### 1. 依存関係の確認
```bash
# Java 17以降
java -version

# Maven 3.6以降
mvn -version

# Docker & Docker Compose (監視機能用)
docker --version
docker-compose --version
```

### 2. プロジェクトのクローン
```bash
git clone <repository-url>
cd microservice-samples
```

### 3. 全サービスのビルド
```bash
# 各サービスディレクトリで実行
for service in inventory-service payment-service shipping-service order-service; do
  cd $service
  ./mvnw clean install
  cd ..
done
```

### 4. サービスの起動
```bash
# 各サービスを別ターミナルで起動
cd inventory-service && ./mvnw spring-boot:run &
cd payment-service && ./mvnw spring-boot:run &
cd shipping-service && ./mvnw spring-boot:run &
cd order-service && ./mvnw spring-boot:run &
```

### 5. 動作確認
```bash
# ヘルスチェック
curl http://localhost:8081/actuator/health  # Inventory Service
curl http://localhost:8082/actuator/health  # Payment Service
curl http://localhost:8083/actuator/health  # Shipping Service
curl http://localhost:8080/actuator/health  # Order Service
```

## 📊 監視・メトリクス

### 監視スタックの起動
```bash
# 監視インフラストラクチャの起動
./shared/scripts/start-monitoring.sh

# アクセス情報
echo "Grafana: http://localhost:3000 (admin/admin123)"
echo "Prometheus: http://localhost:9090"
echo "AlertManager: http://localhost:9093"
echo "Jaeger: http://localhost:16686"
```

### 利用可能なダッシュボード
- **マイクロサービス総合ダッシュボード**
- **分散トランザクション監視**
- **ScalarDB メトリクス**
- **Circuit Breaker 状態**
- **JVM & システムメトリクス**

## 🧪 テスト実行

### 単体テスト
```bash
# 各サービスの単体テスト
./mvnw test
```

### 統合テスト
```bash
# 全サービスの統合テスト
./shared/scripts/run-integration-tests.sh
```

### パフォーマンステスト
```bash
# 負荷テスト実行
./shared/scripts/performance-test.sh
```

## 🔄 API エンドポイント

### Order Process API (分散トランザクション)
```bash
# 注文作成 (Inventory → Payment → Shipping の分散トランザクション)
curl -X POST http://localhost:8080/api/v1/orders \\
  -H "Content-Type: application/json" \\
  -d '{
    "customerId": "CUST-001",
    "items": [{"productId": "PROD-001", "quantity": 1}],
    "paymentMethodDetails": {
      "paymentMethod": "CREDIT_CARD",
      "cardNumber": "4111111111111111",
      "expiryMonth": "12",
      "expiryYear": "2025",
      "cvv": "123",
      "cardholderName": "Test User"
    },
    "shippingInfo": {
      "shippingMethod": "STANDARD",
      "carrier": "YAMATO",
      "recipientInfo": {
        "name": "田中太郎",
        "phone": "090-1234-5678",
        "address": "東京都渋谷区渋谷1-1-1",
        "city": "渋谷区",
        "state": "東京都",
        "postalCode": "150-0002",
        "country": "JP"
      }
    }
  }'

# 注文取得
curl http://localhost:8080/api/v1/orders/{orderId}

# 注文キャンセル (補償トランザクション)
curl -X POST http://localhost:8080/api/v1/orders/{orderId}/cancel
```

### System API エンドポイント

#### Inventory Service
```bash
# 在庫予約
curl -X POST http://localhost:8081/api/v1/inventory/reserve

# 在庫確認
curl "http://localhost:8081/api/v1/inventory/check?productId=PROD-001&quantity=1"
```

#### Payment Service
```bash
# 決済処理
curl -X POST http://localhost:8082/api/v1/payments/process

# 返金処理
curl -X POST http://localhost:8082/api/v1/payments/{paymentId}/refund
```

#### Shipping Service
```bash
# 配送作成
curl -X POST http://localhost:8083/api/v1/shipping/shipments

# 配送状況更新
curl -X POST http://localhost:8083/api/v1/shipping/shipments/{shipmentId}/status
```

### Process API (単機能)
各System APIには、高レベルなビジネスプロセスを実行する単機能Process APIも提供されています：

- `POST /api/v1/inventory/process/reserve-and-confirm` - 予約と即時確定
- `POST /api/v1/payments/process/quick-payment` - 即時決済
- `POST /api/v1/shipping/process/express-delivery` - 特急配送

## 🏛️ 分散トランザクション

### Saga Pattern実装
Order Serviceは以下のステップで分散トランザクションを実行：

1. **在庫予約** (Inventory Service)
2. **決済処理** (Payment Service)  
3. **配送手配** (Shipping Service)
4. **在庫確定** (Inventory Service)

各ステップで障害が発生した場合、自動的に補償処理を実行：
- 配送キャンセル → 決済返金 → 在庫予約キャンセル

### ScalarDB設定
```properties
# ScalarDB 分散トランザクション設定
scalar.db.storage=jdbc
scalar.db.contact_points=jdbc:sqlite:shared/sqlite-data/service.db
scalar.db.transaction_manager=consensus-commit
scalar.db.consensus_commit.isolation_level=SNAPSHOT
```

## 📈 監視・アラート

### メトリクス
- **Business Metrics**: 注文処理数、成功率、処理時間
- **Technical Metrics**: HTTP リクエスト、JVMメモリ、DB接続プール
- **ScalarDB Metrics**: トランザクション処理数、失敗率
- **Circuit Breaker**: 回路状態、失敗率

### アラート
- 高エラー率 (5%以上)
- 高レスポンス時間 (2秒以上)
- サービスダウン
- ScalarDB トランザクション失敗
- Circuit Breaker オープン

## 🛠️ 開発・運用

### ログ設定
```yaml
logging:
  level:
    com.scalar.db: DEBUG
    com.example: DEBUG
    feign: DEBUG
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 設定管理
- **application.yml**: サービス設定
- **scalardb.properties**: ScalarDB設定
- **schema/*.json**: データベーススキーマ

### Docker運用
```bash
# 監視スタック
docker-compose -f shared/docker-compose/docker-compose.monitoring.yml up -d

# テスト環境
docker-compose -f shared/docker-compose/docker-compose.test.yml --profile test up -d
```

## 📚 追加リソース

### ドキュメント
- `docs/api-design-template.md` - API設計テンプレート
- 各サービスの `README.md` - サービス固有の設定

### スクリプト
- `shared/scripts/run-integration-tests.sh` - 統合テスト実行
- `shared/scripts/performance-test.sh` - パフォーマンステスト
- `shared/scripts/start-monitoring.sh` - 監視スタック管理

### 設定ファイル
- `shared/monitoring/` - Prometheus, Grafana, AlertManager設定
- `shared/docker-compose/` - Docker Compose設定

## 🤝 コントリビューション

1. Feature branchを作成
2. 変更を実装
3. テストを追加・実行
4. Pull Requestを作成

## 📄 ライセンス

This project is licensed under the MIT License.