// Service definitions
const services = {
    inventory: {
        name: 'Inventory Service',
        port: 8081,
        baseUrl: 'http://localhost:8081',
        endpoints: [
            { method: 'GET', path: '/actuator/health', description: 'ヘルスチェック' },
            { method: 'GET', path: '/api/inventory', description: '在庫一覧取得' },
            { method: 'GET', path: '/api/inventory/:productId', description: '商品詳細取得' },
            { method: 'PUT', path: '/api/inventory/:productId', description: '在庫更新' },
            { method: 'POST', path: '/api/inventory/reserve', description: '在庫予約' },
            { method: 'PUT', path: '/api/inventory/reservation/:reservationId/confirm', description: '予約確定' },
            { method: 'DELETE', path: '/api/inventory/reservation/:reservationId', description: '予約キャンセル' }
        ]
    },
    payment: {
        name: 'Payment Service',
        port: 8082,
        baseUrl: 'http://localhost:8082',
        endpoints: [
            { method: 'GET', path: '/actuator/health', description: 'ヘルスチェック' },
            { method: 'POST', path: '/api/payments/process', description: '決済処理' },
            { method: 'POST', path: '/api/payments/:paymentId/refund', description: '返金処理' },
            { method: 'GET', path: '/api/payments/customer/:customerId', description: '顧客の決済履歴' }
        ]
    },
    shipping: {
        name: 'Shipping Service',
        port: 8083,
        baseUrl: 'http://localhost:8083',
        endpoints: [
            { method: 'GET', path: '/actuator/health', description: 'ヘルスチェック' },
            { method: 'POST', path: '/api/shipments/create', description: '配送作成' },
            { method: 'PUT', path: '/api/shipments/:shipmentId/status', description: '配送ステータス更新' },
            { method: 'GET', path: '/api/shipments/:shipmentId', description: '配送詳細取得' }
        ]
    },
    order: {
        name: 'Order Service',
        port: 8080,
        baseUrl: 'http://localhost:8080',
        endpoints: [
            { method: 'GET', path: '/actuator/health', description: 'ヘルスチェック' },
            { method: 'POST', path: '/api/orders/process', description: '注文処理' },
            { method: 'GET', path: '/api/orders/:orderId', description: '注文詳細取得' },
            { method: 'GET', path: '/api/orders', description: '注文一覧取得' },
            { method: 'GET', path: '/api/orders/customer/:customerId', description: '顧客の注文履歴' }
        ]
    }
};

// Sample request bodies for different endpoints
const sampleBodies = {
    'POST /api/inventory/reserve': {
        "productId": "ITEM001",
        "quantity": 2,
        "customerId": "CUST-001"
    },
    'PUT /api/inventory/ITEM001': {
        "quantity": 100,
        "reservedQuantity": 5
    },
    'POST /api/payments/process': {
        "orderId": "ORDER-001",
        "customerId": "CUST-001",
        "amount": 160000,
        "paymentMethod": "CREDIT_CARD",
        "currency": "JPY"
    },
    'POST /api/payments/PAY-123/refund': {
        "amount": 50000,
        "reason": "Customer request"
    },
    'POST /api/shipments/create': {
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
    },
    'PUT /api/shipments/SHIP-123/status': {
        "status": "SHIPPED"
    },
    'POST /api/orders/process': {
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
};

let logs = [];

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    initializeServiceGrid();
    setupEventListeners();
    updateServiceStatus();
    
    // Auto-refresh service status every 30 seconds
    setInterval(updateServiceStatus, 30000);
});

function initializeServiceGrid() {
    const grid = document.getElementById('service-grid');
    grid.innerHTML = '';
    
    Object.entries(services).forEach(([key, service]) => {
        const card = createServiceCard(key, service);
        grid.appendChild(card);
    });
}

function createServiceCard(serviceKey, service) {
    const card = document.createElement('div');
    card.className = 'service-card';
    card.innerHTML = `
        <div class="service-header">
            <div class="service-name">
                <span class="status-indicator" id="status-${serviceKey}"></span>
                ${service.name}
            </div>
            <div class="service-port">:${service.port}</div>
        </div>
        <div class="endpoint-list">
            ${service.endpoints.map(endpoint => `
                <div class="endpoint">
                    <div class="endpoint-info">
                        <span class="endpoint-method method-${endpoint.method.toLowerCase()}">${endpoint.method}</span>
                        <span class="endpoint-path">${endpoint.path}</span>
                        <div style="font-size: 0.8rem; color: #888; margin-top: 2px;">${endpoint.description}</div>
                    </div>
                    <button class="test-btn" onclick="testEndpoint('${serviceKey}', '${endpoint.method}', '${endpoint.path}')">
                        テスト
                    </button>
                </div>
            `).join('')}
        </div>
    `;
    return card;
}

function setupEventListeners() {
    const serviceSelect = document.getElementById('service-select');
    const endpointSelect = document.getElementById('endpoint-select');
    const methodSelect = document.getElementById('method-select');
    const urlInput = document.getElementById('url-input');
    const bodyInput = document.getElementById('body-input');
    const form = document.getElementById('api-form');
    
    serviceSelect.addEventListener('change', function() {
        updateEndpointOptions(this.value);
    });
    
    endpointSelect.addEventListener('change', function() {
        updateFormFromEndpoint();
    });
    
    methodSelect.addEventListener('change', function() {
        toggleBodyGroup();
    });
    
    form.addEventListener('submit', function(e) {
        e.preventDefault();
        executeRequest();
    });
    
    // Initialize form state
    toggleBodyGroup();
}

function updateEndpointOptions(serviceKey) {
    const endpointSelect = document.getElementById('endpoint-select');
    endpointSelect.innerHTML = '<option value="">エンドポイントを選択...</option>';
    
    if (serviceKey && services[serviceKey]) {
        services[serviceKey].endpoints.forEach(endpoint => {
            const option = document.createElement('option');
            option.value = `${endpoint.method} ${endpoint.path}`;
            option.textContent = `${endpoint.method} ${endpoint.path} - ${endpoint.description}`;
            endpointSelect.appendChild(option);
        });
    }
}

function updateFormFromEndpoint() {
    const serviceKey = document.getElementById('service-select').value;
    const endpointValue = document.getElementById('endpoint-select').value;
    
    if (serviceKey && endpointValue) {
        const [method, path] = endpointValue.split(' ', 2);
        const service = services[serviceKey];
        
        document.getElementById('method-select').value = method;
        document.getElementById('url-input').value = service.baseUrl + path;
        
        // Set sample body if available
        const bodyKey = endpointValue;
        if (sampleBodies[bodyKey]) {
            document.getElementById('body-input').value = JSON.stringify(sampleBodies[bodyKey], null, 2);
        } else {
            document.getElementById('body-input').value = '';
        }
        
        toggleBodyGroup();
    }
}

function toggleBodyGroup() {
    const method = document.getElementById('method-select').value;
    const bodyGroup = document.getElementById('body-group');
    
    if (method === 'GET' || method === 'DELETE') {
        bodyGroup.style.display = 'none';
    } else {
        bodyGroup.style.display = 'block';
    }
}

function testEndpoint(serviceKey, method, path) {
    const service = services[serviceKey];
    const url = service.baseUrl + path;
    
    // Fill the form
    document.getElementById('service-select').value = serviceKey;
    updateEndpointOptions(serviceKey);
    document.getElementById('endpoint-select').value = `${method} ${path}`;
    document.getElementById('method-select').value = method;
    document.getElementById('url-input').value = url;
    
    // Set sample body if available
    const bodyKey = `${method} ${path}`;
    if (sampleBodies[bodyKey]) {
        document.getElementById('body-input').value = JSON.stringify(sampleBodies[bodyKey], null, 2);
    }
    
    toggleBodyGroup();
    
    // Scroll to form
    document.getElementById('api-form').scrollIntoView({ behavior: 'smooth' });
}

async function executeRequest() {
    const method = document.getElementById('method-select').value;
    const url = document.getElementById('url-input').value;
    const headersText = document.getElementById('headers-input').value.trim();
    const bodyText = document.getElementById('body-input').value.trim();
    
    // Show loading
    showLoading();
    
    const startTime = Date.now();
    
    try {
        // Parse headers
        let headers = { 'Content-Type': 'application/json' };
        if (headersText) {
            try {
                headers = { ...headers, ...JSON.parse(headersText) };
            } catch (e) {
                throw new Error('Invalid headers JSON format');
            }
        }
        
        // Prepare request options
        const options = {
            method: method,
            headers: headers
        };
        
        // Add body for POST/PUT requests
        if ((method === 'POST' || method === 'PUT') && bodyText) {
            try {
                JSON.parse(bodyText); // Validate JSON
                options.body = bodyText;
            } catch (e) {
                throw new Error('Invalid request body JSON format');
            }
        }
        
        // Execute request
        const response = await fetch(url, options);
        const responseTime = Date.now() - startTime;
        
        // Parse response
        let responseBody;
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            responseBody = await response.json();
        } else {
            responseBody = await response.text();
        }
        
        // Show response
        showResponse({
            status: response.status,
            statusText: response.statusText,
            headers: Object.fromEntries(response.headers.entries()),
            body: responseBody,
            responseTime: responseTime
        });
        
        // Log request
        logRequest(method, url, response.status, responseTime);
        
    } catch (error) {
        const responseTime = Date.now() - startTime;
        
        showResponse({
            status: 0,
            statusText: 'Error',
            headers: {},
            body: error.message,
            responseTime: responseTime,
            error: true
        });
        
        logRequest(method, url, 'ERROR', responseTime, error.message);
    }
}

function showLoading() {
    const container = document.getElementById('response-container');
    container.style.display = 'block';
    container.innerHTML = `
        <div class="loading">
            <div class="spinner"></div>
            <div>リクエスト送信中...</div>
        </div>
    `;
}

function showResponse(response) {
    const container = document.getElementById('response-container');
    const isSuccess = response.status >= 200 && response.status < 300;
    const statusClass = response.error || !isSuccess ? 'status-error' : 'status-success';
    
    container.style.display = 'block';
    container.innerHTML = `
        <div class="response-header">
            <h3>📨 レスポンス</h3>
            <div class="response-status ${statusClass}">
                ${response.status} ${response.statusText}
            </div>
            <div style="color: #666; font-size: 0.9rem;">
                ${response.responseTime}ms
            </div>
        </div>
        <div class="response-body">${typeof response.body === 'object' ? JSON.stringify(response.body, null, 2) : response.body}</div>
    `;
}

function logRequest(method, url, status, responseTime, error = null) {
    const timestamp = new Date().toLocaleTimeString('ja-JP');
    const logEntry = {
        timestamp,
        method,
        url,
        status,
        responseTime,
        error
    };
    
    logs.unshift(logEntry);
    if (logs.length > 100) {
        logs = logs.slice(0, 100); // Keep only last 100 logs
    }
    
    updateLogsDisplay();
}

function updateLogsDisplay() {
    const logsContainer = document.getElementById('logs');
    logsContainer.innerHTML = logs.map(log => `
        <div class="log-entry">
            <span class="log-time">${log.timestamp}</span>
            <span class="log-method">${log.method}</span>
            <span class="log-url">${log.url}</span>
            <span style="color: ${log.error ? '#f44336' : (log.status >= 200 && log.status < 300 ? '#4CAF50' : '#FF9800')}">
                ${log.status} (${log.responseTime}ms)
            </span>
            ${log.error ? `<span style="color: #f44336; margin-left: 10px;">- ${log.error}</span>` : ''}
        </div>
    `).join('');
}

function clearLogs() {
    logs = [];
    updateLogsDisplay();
}

async function updateServiceStatus() {
    for (const [key, service] of Object.entries(services)) {
        const statusIndicator = document.getElementById(`status-${key}`);
        
        try {
            const response = await fetch(`${service.baseUrl}/actuator/health`, {
                method: 'GET',
                timeout: 5000
            });
            
            if (response.ok) {
                statusIndicator.className = 'status-indicator status-up';
                statusIndicator.title = 'Service is running';
            } else {
                statusIndicator.className = 'status-indicator status-down';
                statusIndicator.title = 'Service is not responding properly';
            }
        } catch (error) {
            statusIndicator.className = 'status-indicator status-down';
            statusIndicator.title = 'Service is not reachable';
        }
    }
}

// Export functions for global access
window.testEndpoint = testEndpoint;
window.clearLogs = clearLogs;