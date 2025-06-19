// Demo Application JavaScript
const API_BASE_URL = '';

// Current data cache
let currentInventory = [];
let currentWallet = null;

// Tab management
function showTab(tabName) {
    // Hide all tab panes
    document.querySelectorAll('.tab-pane').forEach(pane => {
        pane.classList.remove('active');
    });
    
    // Remove active class from all tabs
    document.querySelectorAll('.tab').forEach(tab => {
        tab.classList.remove('active');
    });
    
    // Show selected tab pane
    document.getElementById(`${tabName}-tab`).classList.add('active');
    
    // Add active class to clicked tab
    event.target.classList.add('active');
    
    // Auto-load data for certain tabs
    if (tabName === 'inventory') {
        loadInventory();
    }
}

// Utility functions
function showLoading(containerId) {
    const container = document.getElementById(containerId);
    container.style.display = 'block';
    container.innerHTML = `
        <div class="loading">
            <div class="spinner"></div>
            <div>処理中...</div>
        </div>
    `;
}

function showResult(containerId, data, success = true) {
    const container = document.getElementById(containerId);
    const statusClass = success ? 'status-success' : 'status-error';
    
    container.style.display = 'block';
    container.innerHTML = `
        <div class="${statusClass}" style="padding: 15px; border-radius: 8px; margin-bottom: 15px;">
            <h4>${success ? '✅ 成功' : '❌ エラー'}</h4>
        </div>
        <pre>${JSON.stringify(data, null, 2)}</pre>
    `;
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('ja-JP', {
        style: 'currency',
        currency: 'JPY'
    }).format(amount);
}

function formatDateTime(dateString) {
    return new Date(dateString).toLocaleString('ja-JP');
}

// Inventory Management Functions
async function loadInventory() {
    showLoading('inventory-list');
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/inventory`);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        const contentType = response.headers.get('content-type');
        let inventory;
        
        if (contentType && contentType.includes('application/json')) {
            inventory = await response.json();
        } else {
            const text = await response.text();
            throw new Error(`Expected JSON response, got: ${text.substring(0, 100)}`);
        }
        
        currentInventory = inventory;
        displayInventory(inventory);
    } catch (error) {
        console.error('Load inventory error:', error);
        document.getElementById('inventory-list').innerHTML = `
            <div class="status-error" style="padding: 15px; margin-top: 15px;">
                <p>❌ 在庫データの取得に失敗しました: ${error.message}</p>
            </div>
        `;
    }
}

function displayInventory(inventory) {
    const container = document.getElementById('inventory-list');
    
    if (!inventory || inventory.length === 0) {
        container.innerHTML = '<p style="margin-top: 15px; color: #666;">在庫データがありません</p>';
        return;
    }
    
    container.innerHTML = `
        <div style="margin-top: 15px;">
            ${inventory.map(item => `
                <div class="inventory-item">
                    <div>
                        <h4>${item.productName}</h4>
                        <div class="details">
                            ID: ${item.productId} | カテゴリ: ${item.category}<br>
                            単価: ${formatCurrency(item.unitPrice)}
                        </div>
                    </div>
                    <div class="stock">
                        <div class="${item.availableQuantity <= item.lowStockThreshold ? 'stock-low' : 'stock-ok'}">
                            在庫: ${item.availableQuantity}個
                        </div>
                        <div style="font-size: 0.8rem; color: #666;">
                            (総数: ${item.quantity}, 予約: ${item.reservedQuantity})
                        </div>
                    </div>
                </div>
            `).join('')}
        </div>
    `;
}

async function restockInventory() {
    const productId = document.getElementById('restock-product').value;
    const quantity = parseInt(document.getElementById('restock-quantity').value);
    const supplier = document.getElementById('restock-supplier').value;
    const cost = parseInt(document.getElementById('restock-cost').value) || 0;
    
    if (!productId || !quantity || quantity <= 0) {
        showResult('inventory-result', { error: '商品IDと正の数量を入力してください' }, false);
        return;
    }
    
    showLoading('inventory-result');
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/inventory/${productId}/restock`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                quantity,
                supplier,
                cost
            })
        });
        
        let result;
        const contentType = response.headers.get('content-type');
        
        if (contentType && contentType.includes('application/json')) {
            result = await response.json();
        } else {
            const text = await response.text();
            result = { error: `Non-JSON response: ${text}` };
        }
        
        if (response.ok) {
            showResult('inventory-result', result, true);
            // Refresh inventory display
            loadInventory();
        } else {
            showResult('inventory-result', result, false);
        }
    } catch (error) {
        console.error('Restock error:', error);
        showResult('inventory-result', { error: error.message }, false);
    }
}

// Wallet Management Functions
async function checkWalletBalance() {
    const customerId = document.getElementById('wallet-customer').value;
    
    if (!customerId) {
        showResult('wallet-result', { error: '顧客IDを選択してください' }, false);
        return;
    }
    
    showLoading('wallet-balance-display');
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/wallet/${customerId}`);
        
        if (response.ok) {
            const contentType = response.headers.get('content-type');
            let wallet;
            
            if (contentType && contentType.includes('application/json')) {
                wallet = await response.json();
            } else {
                throw new Error('Expected JSON response from wallet API');
            }
            
            currentWallet = wallet;
            displayWalletBalance(wallet);
        } else {
            let error;
            const contentType = response.headers.get('content-type');
            
            if (contentType && contentType.includes('application/json')) {
                error = await response.json();
            } else {
                const text = await response.text();
                error = { message: text || `HTTP ${response.status}` };
            }
            
            document.getElementById('wallet-balance-display').innerHTML = `
                <div class="status-error" style="padding: 15px; margin-top: 15px;">
                    <p>❌ ${error.message}</p>
                </div>
            `;
        }
    } catch (error) {
        console.error('Wallet balance error:', error);
        document.getElementById('wallet-balance-display').innerHTML = `
            <div class="status-error" style="padding: 15px; margin-top: 15px;">
                <p>❌ ウォレット情報の取得に失敗しました: ${error.message}</p>
            </div>
        `;
    }
}

function displayWalletBalance(wallet) {
    const container = document.getElementById('wallet-balance-display');
    
    container.innerHTML = `
        <div class="wallet-balance" style="margin-top: 15px;">
            <h2>${formatCurrency(wallet.balance)}</h2>
            <p>顧客ID: ${wallet.customerId}</p>
            <p>最終更新: ${formatDateTime(wallet.lastUpdated)}</p>
        </div>
    `;
}

async function addFunds() {
    const customerId = document.getElementById('add-funds-customer').value;
    const amount = parseInt(document.getElementById('add-amount').value);
    const source = document.getElementById('funding-source').value;
    
    if (!customerId || !amount || amount <= 0) {
        showResult('wallet-result', { error: '顧客IDと正の金額を入力してください' }, false);
        return;
    }
    
    showLoading('wallet-result');
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/wallet/${customerId}/add-funds`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                amount,
                source
            })
        });
        
        let result;
        const contentType = response.headers.get('content-type');
        
        if (contentType && contentType.includes('application/json')) {
            result = await response.json();
        } else {
            const text = await response.text();
            result = { error: `Non-JSON response: ${text}` };
        }
        
        if (response.ok) {
            showResult('wallet-result', result, true);
            // Update wallet display if it's the same customer
            const currentCustomer = document.getElementById('wallet-customer').value;
            if (currentCustomer === customerId) {
                displayWalletBalance(result);
            }
        } else {
            showResult('wallet-result', result, false);
        }
    } catch (error) {
        console.error('Add funds error:', error);
        showResult('wallet-result', { error: error.message }, false);
    }
}

// Order Processing Functions
function addOrderItem() {
    const container = document.getElementById('order-items');
    const newItem = document.createElement('div');
    newItem.className = 'order-item-row';
    newItem.style.cssText = 'display: flex; gap: 10px; margin-bottom: 10px; align-items: center;';
    
    newItem.innerHTML = `
        <select class="form-control" style="flex: 2;">
            <option value="ITEM001">ITEM001 - ノートパソコン (¥80,000)</option>
            <option value="ITEM002">ITEM002 - スマートフォン (¥60,000)</option>
            <option value="ITEM003">ITEM003 - マウス (¥2,500)</option>
        </select>
        <input type="number" class="form-control" placeholder="数量" value="1" min="1" style="flex: 1;">
        <button class="btn btn-warning" onclick="removeOrderItem(this)">削除</button>
    `;
    
    container.appendChild(newItem);
}

function removeOrderItem(button) {
    button.parentElement.remove();
}

async function processOrder() {
    const customerId = document.getElementById('order-customer').value;
    const paymentMethod = document.querySelector('input[name="payment-method"]:checked').value;
    const useWallet = paymentMethod === 'wallet';
    
    // Collect shipping address
    const shippingAddress = {
        name: document.getElementById('shipping-name').value,
        postalCode: document.getElementById('shipping-postal').value,
        prefecture: '東京都',
        city: '千代田区',
        address: document.getElementById('shipping-address').value,
        phone: document.getElementById('shipping-phone').value
    };
    
    // Collect order items
    const itemRows = document.querySelectorAll('.order-item-row');
    const items = [];
    
    for (const row of itemRows) {
        const select = row.querySelector('select');
        const quantityInput = row.querySelector('input[type="number"]');
        
        if (select.value && quantityInput.value) {
            items.push({
                productId: select.value,
                quantity: parseInt(quantityInput.value)
            });
        }
    }
    
    // Validation
    if (!customerId || !shippingAddress.name || !shippingAddress.address || items.length === 0) {
        showResult('order-result', { error: '必要な情報をすべて入力してください' }, false);
        return;
    }
    
    showLoading('order-result');
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/orders/demo-process`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                customerId,
                items,
                shippingAddress,
                useWallet
            })
        });
        
        let result;
        const contentType = response.headers.get('content-type');
        
        if (contentType && contentType.includes('application/json')) {
            result = await response.json();
        } else {
            const text = await response.text();
            result = { error: `Non-JSON response: ${text}` };
        }
        
        if (response.ok) {
            displayOrderResult(result);
        } else {
            showResult('order-result', result, false);
        }
    } catch (error) {
        console.error('Process order error:', error);
        showResult('order-result', { error: error.message }, false);
    }
}

function displayOrderResult(order) {
    const container = document.getElementById('order-result');
    
    container.style.display = 'block';
    container.innerHTML = `
        <div class="status-success" style="padding: 15px; border-radius: 8px; margin-bottom: 15px;">
            <h4>✅ 注文が正常に処理されました</h4>
        </div>
        
        <div class="order-summary">
            <h3>📋 注文詳細</h3>
            <div class="order-item">
                <span>注文ID:</span>
                <span>${order.orderId}</span>
            </div>
            <div class="order-item">
                <span>顧客ID:</span>
                <span>${order.customerId}</span>
            </div>
            <div class="order-item">
                <span>決済方法:</span>
                <span>${order.paymentMethod === 'WALLET' ? 'ウォレット' : 'クレジットカード'}</span>
            </div>
            <div class="order-item">
                <span>追跡番号:</span>
                <span>${order.shipment.trackingNumber}</span>
            </div>
            <div class="order-item">
                <span>合計金額:</span>
                <span>${formatCurrency(order.totalAmount)}</span>
            </div>
        </div>
        
        <div style="margin-top: 15px; padding: 15px; background: #d1ecf1; border-radius: 8px;">
            <p><strong>📍 追跡番号: ${order.shipment.trackingNumber}</strong></p>
            <p>この番号で配送状況を追跡できます。</p>
        </div>
    `;
}

// Shipping Tracking Functions
async function trackShipment() {
    const trackingNumber = document.getElementById('tracking-number').value.trim();
    
    if (!trackingNumber) {
        showResult('tracking-result', { error: '追跡番号を入力してください' }, false);
        return;
    }
    
    showLoading('tracking-result');
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/shipments/track/${trackingNumber}`);
        
        if (response.ok) {
            const contentType = response.headers.get('content-type');
            let tracking;
            
            if (contentType && contentType.includes('application/json')) {
                tracking = await response.json();
            } else {
                throw new Error('Expected JSON response from tracking API');
            }
            
            displayTrackingResult(tracking);
        } else {
            let error;
            const contentType = response.headers.get('content-type');
            
            if (contentType && contentType.includes('application/json')) {
                error = await response.json();
            } else {
                const text = await response.text();
                error = { error: text || `HTTP ${response.status}` };
            }
            
            showResult('tracking-result', error, false);
        }
    } catch (error) {
        console.error('Track shipment error:', error);
        showResult('tracking-result', { error: error.message }, false);
    }
}

function displayTrackingResult(tracking) {
    const container = document.getElementById('tracking-result');
    
    container.style.display = 'block';
    container.innerHTML = `
        <div class="status-success" style="padding: 15px; border-radius: 8px; margin-bottom: 15px;">
            <h4>📦 配送状況</h4>
            <p><strong>追跡番号:</strong> ${tracking.trackingNumber}</p>
            <p><strong>配送業者:</strong> ${tracking.carrier}</p>
            <p><strong>現在のステータス:</strong> ${getStatusText(tracking.currentStatus)}</p>
            <p><strong>予定配送日:</strong> ${formatDateTime(tracking.estimatedDelivery)}</p>
        </div>
        
        <div style="background: white; padding: 20px; border-radius: 10px;">
            <h4>📍 配送履歴</h4>
            ${tracking.trackingHistory.map((step, index) => `
                <div class="tracking-step ${getStepClass(step.status, tracking.currentStatus)}">
                    <div class="tracking-icon">${index + 1}</div>
                    <div>
                        <div style="font-weight: bold;">${step.description}</div>
                        <div style="color: #666; font-size: 0.9rem;">
                            ${formatDateTime(step.timestamp)} - ${step.location}
                        </div>
                    </div>
                </div>
            `).join('')}
        </div>
    `;
}

function getStatusText(status) {
    const statusMap = {
        'PENDING': '準備中',
        'PROCESSING': '処理中', 
        'SHIPPED': '発送済み',
        'DELIVERED': '配送完了',
        'CANCELLED': 'キャンセル'
    };
    return statusMap[status] || status;
}

function getStepClass(stepStatus, currentStatus) {
    const statusOrder = ['PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED'];
    const stepIndex = statusOrder.indexOf(stepStatus);
    const currentIndex = statusOrder.indexOf(currentStatus);
    
    if (stepIndex < currentIndex) return 'completed';
    if (stepIndex === currentIndex) return 'current';
    return '';
}

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    // Auto-load inventory on page load
    loadInventory();
    
    console.log('📱 ScalarDB マイクロサービス デモアプリケーション が開始されました');
    console.log('🔧 使用可能な機能:');
    console.log('   - 📦 在庫管理・補充');
    console.log('   - 💰 ウォレット残高管理');
    console.log('   - 🛒 注文処理 (ウォレット・クレジットカード対応)');
    console.log('   - 🚚 配送状況追跡');
});