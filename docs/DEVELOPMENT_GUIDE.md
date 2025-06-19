# 開発者ガイド - ScalarDB マイクロサービス

## 🎯 開発環境セットアップ

### 必要な環境
- **Node.js**: 14.0.0 以上
- **npm**: 6.0.0 以上
- **Git**: 2.0 以上
- **VSCode**: 推奨エディタ

### プロジェクト構造
```
microservice-samples/
├── api-tester/                 # API テスター & プロキシサーバー
│   ├── index.html             # API テスト UI
│   ├── demo.html              # デモアプリケーション UI
│   ├── demo.js                # デモアプリケーション ロジック
│   ├── api-tester.js          # API テスター ロジック
│   ├── server.js              # Express プロキシサーバー
│   └── package.json           # 依存関係定義
├── test/                      # Mock サーバー
│   └── mock-server.js         # 全マイクロサービス実装
├── logs/                      # ログファイル
│   └── mock-server.log        # Mock サーバーログ
├── docs/                      # ドキュメント
│   ├── README.md              # プロジェクト概要
│   ├── API_SPECIFICATION.md   # API 仕様書
│   ├── ARCHITECTURE.md        # アーキテクチャ設計書
│   └── DEVELOPMENT_GUIDE.md   # 開発者ガイド (このファイル)
└── CLAUDE.md                  # Claude Code プロジェクト設定
```

### 初期セットアップ
```bash
# 1. リポジトリクローン
git clone <repository-url>
cd microservice-samples

# 2. API Tester 依存関係インストール
cd api-tester
npm install

# 3. 環境確認
node --version  # 14.0.0+ が表示されることを確認
npm --version   # 6.0.0+ が表示されることを確認
```

## 🚀 開発サーバー起動

### 1. Mock Server 起動
```bash
# バックグラウンドで起動
cd microservice-samples/test
node mock-server.js > ../logs/mock-server.log 2>&1 &

# または、フォアグラウンドで起動 (デバッグ用)
node mock-server.js
```

### 2. API Tester & Demo App 起動
```bash
cd microservice-samples/api-tester
npm start
```

### 3. アクセス確認
- **API テスター**: http://localhost:3000
- **デモアプリ**: http://localhost:3000/demo
- **API ドキュメント**: http://localhost:3000/api/docs

### 4. ヘルスチェック
```bash
# 全サービスが正常に起動していることを確認
curl http://localhost:8080/actuator/health  # Order Service
curl http://localhost:8081/actuator/health  # Inventory Service
curl http://localhost:8082/actuator/health  # Payment Service
curl http://localhost:8083/actuator/health  # Shipping Service
```

## 🧪 テスト手順

### 機能テスト

#### 1. 在庫管理テスト
```bash
# 在庫一覧取得
curl http://localhost:3000/api/inventory

# 在庫補充
curl -X POST http://localhost:3000/api/inventory/ITEM001/restock \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 10,
    "supplier": "TEST_SUPPLIER",
    "cost": 50000
  }'
```

#### 2. ウォレット管理テスト
```bash
# ウォレット残高確認
curl http://localhost:3000/api/wallet/DEMO-USER-001

# 資金追加
curl -X POST http://localhost:3000/api/wallet/DEMO-USER-001/add-funds \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 10000,
    "source": "BANK_TRANSFER"
  }'
```

#### 3. 注文処理テスト
```bash
# デモ注文処理
curl -X POST http://localhost:3000/api/orders/demo-process \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "DEMO-USER-001",
    "items": [
      {
        "productId": "ITEM001",
        "quantity": 1
      }
    ],
    "shippingAddress": {
      "name": "テスト太郎",
      "postalCode": "100-0001",
      "prefecture": "東京都",
      "city": "千代田区",
      "address": "千代田1-1-1",
      "phone": "03-1234-5678"
    },
    "useWallet": true
  }'
```

#### 4. 配送追跡テスト
```bash
# 追跡番号で配送状況確認
curl http://localhost:3000/api/shipments/track/DEMO1750242000000
```

### 統合テスト

#### エンドツーエンドシナリオ
1. **在庫確認** → 在庫一覧取得
2. **ウォレット準備** → 資金追加
3. **注文処理** → デモ注文実行
4. **配送追跡** → 追跡番号での確認

```bash
#!/bin/bash
# E2E テストスクリプト例

# 1. 在庫確認
echo "1. 在庫確認中..."
curl -s http://localhost:3000/api/inventory | jq .

# 2. ウォレット資金追加
echo "2. ウォレット資金追加中..."
curl -s -X POST http://localhost:3000/api/wallet/DEMO-USER-001/add-funds \
  -H "Content-Type: application/json" \
  -d '{"amount": 100000, "source": "BANK_TRANSFER"}' | jq .

# 3. 注文処理
echo "3. 注文処理中..."
ORDER_RESPONSE=$(curl -s -X POST http://localhost:3000/api/orders/demo-process \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "DEMO-USER-001",
    "items": [{"productId": "ITEM001", "quantity": 1}],
    "shippingAddress": {
      "name": "テスト太郎",
      "postalCode": "100-0001",
      "prefecture": "東京都",
      "city": "千代田区",
      "address": "千代田1-1-1",
      "phone": "03-1234-5678"
    },
    "useWallet": true
  }')

echo $ORDER_RESPONSE | jq .

# 4. 追跡番号取得と配送確認
TRACKING_NUMBER=$(echo $ORDER_RESPONSE | jq -r '.shipment.trackingNumber')
echo "4. 配送追跡中... (追跡番号: $TRACKING_NUMBER)"
curl -s http://localhost:3000/api/shipments/track/$TRACKING_NUMBER | jq .

echo "E2E テスト完了"
```

## 🔧 開発ワークフロー

### 新機能開発手順

#### 1. フィーチャーブランチ作成
```bash
git checkout -b feature/新機能名
```

#### 2. 開発環境での実装
- Mock Server (`test/mock-server.js`) にAPIエンドポイント追加
- フロントエンド (`api-tester/demo.js`) にUI機能追加
- テストケース作成

#### 3. 実装例: 新しいAPIエンドポイント追加

**Mock Server に新エンドポイント追加:**
```javascript
// test/mock-server.js
inventoryApp.get('/api/inventory/categories', (req, res) => {
  const categories = [...new Set(Array.from(inventoryItems.values()).map(item => item.category))];
  res.json(categories);
});
```

**フロントエンドに機能追加:**
```javascript
// api-tester/demo.js
async function loadCategories() {
  try {
    const response = await fetch(`${API_BASE_URL}/api/inventory/categories`);
    const categories = await response.json();
    // UI 更新処理
  } catch (error) {
    console.error('Categories load error:', error);
  }
}
```

#### 4. テスト実行
```bash
# 手動テスト
curl http://localhost:3000/api/inventory/categories

# ブラウザでの動作確認
open http://localhost:3000/demo
```

#### 5. コミット・プッシュ
```bash
git add .
git commit -m "feat: Add category listing API and UI"
git push origin feature/新機能名
```

### コードレビュー観点

#### API設計チェックリスト
- [ ] RESTful な URL 設計
- [ ] 適切な HTTP メソッド使用
- [ ] JSON レスポンス形式の一貫性
- [ ] エラーハンドリングの実装
- [ ] 入力値検証の実装

#### セキュリティチェックリスト
- [ ] SQLインジェクション対策
- [ ] XSS対策 (フロントエンド)
- [ ] CSRF対策
- [ ] 適切な CORS 設定
- [ ] 機密情報のログ出力回避

#### パフォーマンスチェックリスト
- [ ] 不要な N+1 クエリの回避
- [ ] 適切なページング実装
- [ ] レスポンス時間の最適化
- [ ] メモリリークの確認

## 🐛 デバッグ・トラブルシューティング

### よくある問題と解決法

#### 1. 504 Gateway Timeout エラー
**症状**: API 呼び出し時に 504 エラー

**原因**: Mock Server が停止している

**解決法**:
```bash
# Mock Server 再起動
cd microservice-samples/test
node mock-server.js > ../logs/mock-server.log 2>&1 &

# プロセス確認
ps aux | grep mock-server
```

#### 2. CORS エラー
**症状**: ブラウザコンソールに CORS エラー

**原因**: 直接サービスにアクセスしている

**解決法**:
```bash
# プロキシサーバー経由でアクセス
# ✗ http://localhost:8081/api/inventory
# ✓ http://localhost:3000/api/inventory
```

#### 3. JSON Parse エラー
**症状**: "Unexpected token" エラー

**原因**: サーバーが HTML レスポンスを返している

**解決法**:
- ネットワークタブでレスポンス内容確認
- API エンドポイントの実装確認
- Content-Type ヘッダー確認

#### 4. ポート競合エラー
**症状**: "EADDRINUSE" エラー

**原因**: ポートが既に使用されている

**解決法**:
```bash
# 使用中のポート確認
lsof -i :3000
lsof -i :8080

# プロセス終了
kill -9 <PID>

# または、別ポートを使用
PORT=3001 npm start
```

### ログ確認方法

#### Mock Server ログ
```bash
# リアルタイムログ監視
tail -f logs/mock-server.log

# 最新100行表示
tail -n 100 logs/mock-server.log

# エラーログのみ抽出
grep -i error logs/mock-server.log
```

#### ブラウザデバッグ
```javascript
// デモアプリでのデバッグ
// ブラウザコンソールで実行可能

// API 直接呼び出し
fetch('/api/inventory').then(r => r.json()).then(console.log);

// エラー詳細確認
window.addEventListener('error', (e) => {
  console.error('Global error:', e);
});

// ネットワーク監視
performance.getEntriesByType('navigation').forEach(console.log);
```

### デバッグツール

#### 1. API テスター使用
- ブラウザで http://localhost:3000 にアクセス
- 各エンドポイントを個別テスト
- レスポンス内容とステータスコード確認

#### 2. cURL による直接テスト
```bash
# 詳細なレスポンス確認
curl -v http://localhost:3000/api/inventory

# タイミング測定
curl -w "@curl-format.txt" -o /dev/null -s http://localhost:3000/api/inventory

# curl-format.txt 内容例:
#     time_namelookup:  %{time_namelookup}\n
#     time_connect:     %{time_connect}\n
#     time_appconnect:  %{time_appconnect}\n
#     time_pretransfer: %{time_pretransfer}\n
#     time_redirect:    %{time_redirect}\n
#     time_starttransfer: %{time_starttransfer}\n
#     time_total:       %{time_total}\n
```

#### 3. ブラウザ開発者ツール
- **Network**: API 呼び出し詳細確認
- **Console**: JavaScript エラー確認
- **Application**: LocalStorage/SessionStorage 確認
- **Performance**: パフォーマンス分析

## 📊 コード品質管理

### ESLint 設定 (推奨)
```json
{
  "extends": ["eslint:recommended"],
  "env": {
    "node": true,
    "es6": true,
    "browser": true
  },
  "rules": {
    "no-console": "warn",
    "no-unused-vars": "error",
    "semi": ["error", "always"],
    "quotes": ["error", "single"]
  }
}
```

### Prettier 設定 (推奨)
```json
{
  "semi": true,
  "trailingComma": "es5",
  "singleQuote": true,
  "printWidth": 100,
  "tabWidth": 2
}
```

### Git Hooks (推奨)
```bash
# .git/hooks/pre-commit
#!/bin/sh
npm run lint
npm run test
```

## 🚀 デプロイメント

### 開発環境デプロイ
```bash
# 1. 依存関係更新
npm install

# 2. ビルド (必要に応じて)
npm run build

# 3. サーバー起動
npm start
```

### 本番環境準備
```dockerfile
# Dockerfile 例
FROM node:18-alpine

WORKDIR /app

# 依存関係のみ先にコピー (キャッシュ効率化)
COPY package*.json ./
RUN npm ci --only=production

# アプリケーションコード
COPY . .

EXPOSE 3000
CMD ["npm", "start"]
```

### Docker Compose 設定
```yaml
# docker-compose.yml
version: '3.8'
services:
  mock-server:
    build: .
    ports:
      - "8080:8080"
      - "8081:8081"
      - "8082:8082"
      - "8083:8083"
    volumes:
      - ./logs:/app/logs
    command: node test/mock-server.js

  api-tester:
    build:
      context: ./api-tester
    ports:
      - "3000:3000"
    depends_on:
      - mock-server
    environment:
      - NODE_ENV=production
```

## 📋 開発標準・規約

### ファイル命名規約
- **JavaScript**: kebab-case (例: `demo-app.js`)
- **HTML**: kebab-case (例: `demo-app.html`)
- **CSS**: kebab-case (例: `demo-styles.css`)
- **ディレクトリ**: kebab-case (例: `api-tester`)

### コミットメッセージ規約
```
type(scope): description

例:
feat(inventory): Add inventory restocking API
fix(payment): Fix wallet balance calculation bug
docs(api): Update API specification
test(order): Add E2E test for order processing
```

### API 設計規約
- **URL**: RESTful 設計、複数形を使用
- **HTTP メソッド**: GET (取得), POST (作成), PUT (更新), DELETE (削除)
- **ステータスコード**: 適切な HTTP ステータスコード使用
- **エラーレスポンス**: 統一された error オブジェクト形式

### セキュリティ規約
- **パスワード**: 平文保存禁止
- **API キー**: 環境変数で管理
- **ログ**: 個人情報・機密情報の出力禁止
- **入力値検証**: 全ての入力に対して実施

## 🤝 チーム開発

### ブランチ戦略
```
main
  ├── develop
  │   ├── feature/inventory-enhancement
  │   ├── feature/payment-wallet
  │   └── feature/shipping-tracking
  └── hotfix/critical-bug-fix
```

### プルリクエスト手順
1. **Feature ブランチ作成**
2. **実装・テスト**
3. **プルリクエスト作成**
4. **コードレビュー**
5. **CI/CD 通過確認**
6. **develop ブランチへマージ**

### レビュー観点
- [ ] **機能要件**: 仕様通りに動作するか
- [ ] **性能要件**: レスポンス時間は適切か
- [ ] **セキュリティ**: 脆弱性はないか
- [ ] **保守性**: コードは読みやすく保守しやすいか
- [ ] **テスト**: 適切なテストが書かれているか

## 📚 参考資料

### 公式ドキュメント
- [ScalarDB OSS Documentation](https://scalardl.readthedocs.io/)
- [Express.js Documentation](https://expressjs.com/)
- [Node.js Documentation](https://nodejs.org/docs/)

### プロジェクト固有ドキュメント
- [README.md](./README.md) - プロジェクト概要
- [API_SPECIFICATION.md](./API_SPECIFICATION.md) - API 仕様書
- [ARCHITECTURE.md](./ARCHITECTURE.md) - アーキテクチャ設計書

### 開発ツール
- [Postman](https://www.postman.com/) - API テスト
- [VSCode](https://code.visualstudio.com/) - エディタ
- [Git](https://git-scm.com/) - バージョン管理

---

## 🆘 サポート

### 問い合わせ先
- **技術的質問**: GitHub Issues
- **緊急事態**: Slack #microservices-dev
- **ドキュメント修正**: プルリクエスト歓迎

### よくある質問 (FAQ)

**Q: 新しいマイクロサービスを追加するには？**
A: `mock-server.js` に新しい Express アプリを作成し、適切なポートで listen してください。

**Q: データベーススキーマを変更するには？**
A: Mock Server 内のデータ構造とバリデーションロジックを更新してください。

**Q: 本番環境での ScalarDB 設定は？**
A: CLAUDE.md の設定例を参考に、実際の ScalarDB クラスターへの接続設定を行ってください。

---

**最終更新日**: 2025年6月18日  
**バージョン**: 1.0.0  
**作成者**: Claude Code Development Team