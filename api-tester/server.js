const express = require('express');
const path = require('path');
const { createProxyMiddleware } = require('http-proxy-middleware');

const app = express();
const PORT = 3000;

// Serve static files
app.use(express.static(__dirname));

// CORS middleware
app.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization');
    
    if (req.method === 'OPTIONS') {
        res.sendStatus(200);
    } else {
        next();
    }
});

// API Proxies to avoid CORS issues
app.use('/api/inventory', createProxyMiddleware({
    target: 'http://localhost:8081',
    changeOrigin: true,
    pathRewrite: {
        '^/api/inventory': '/api/inventory'
    }
}));

app.use('/api/payments', createProxyMiddleware({
    target: 'http://localhost:8082',
    changeOrigin: true,
    pathRewrite: {
        '^/api/payments': '/api/payments'
    }
}));

app.use('/api/wallet', createProxyMiddleware({
    target: 'http://localhost:8082',
    changeOrigin: true,
    pathRewrite: {
        '^/api/wallet': '/api/wallet'
    }
}));

app.use('/api/shipments', createProxyMiddleware({
    target: 'http://localhost:8083',
    changeOrigin: true,
    pathRewrite: {
        '^/api/shipments': '/api/shipments'
    }
}));

app.use('/api/orders', createProxyMiddleware({
    target: 'http://localhost:8080',
    changeOrigin: true,
    pathRewrite: {
        '^/api/orders': '/api/orders'
    }
}));

// Health check proxies
app.use('/actuator/health/inventory', createProxyMiddleware({
    target: 'http://localhost:8081',
    changeOrigin: true,
    pathRewrite: {
        '^/actuator/health/inventory': '/actuator/health'
    }
}));

app.use('/actuator/health/payment', createProxyMiddleware({
    target: 'http://localhost:8082',
    changeOrigin: true,
    pathRewrite: {
        '^/actuator/health/payment': '/actuator/health'
    }
}));

app.use('/actuator/health/shipping', createProxyMiddleware({
    target: 'http://localhost:8083',
    changeOrigin: true,
    pathRewrite: {
        '^/actuator/health/shipping': '/actuator/health'
    }
}));

app.use('/actuator/health/order', createProxyMiddleware({
    target: 'http://localhost:8080',
    changeOrigin: true,
    pathRewrite: {
        '^/actuator/health/order': '/actuator/health'
    }
}));

// Main route
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

// Demo route
app.get('/demo', (req, res) => {
    res.sendFile(path.join(__dirname, 'demo.html'));
});

// API documentation endpoint
app.get('/api/docs', (req, res) => {
    res.json({
        message: 'Microservices API Documentation',
        services: {
            inventory: {
                baseUrl: 'http://localhost:8081',
                endpoints: [
                    'GET /actuator/health',
                    'GET /api/inventory',
                    'GET /api/inventory/:productId',
                    'PUT /api/inventory/:productId',
                    'POST /api/inventory/reserve',
                    'PUT /api/inventory/reservation/:reservationId/confirm',
                    'DELETE /api/inventory/reservation/:reservationId'
                ]
            },
            payment: {
                baseUrl: 'http://localhost:8082',
                endpoints: [
                    'GET /actuator/health',
                    'POST /api/payments/process',
                    'POST /api/payments/:paymentId/refund',
                    'GET /api/payments/customer/:customerId'
                ]
            },
            shipping: {
                baseUrl: 'http://localhost:8083',
                endpoints: [
                    'GET /actuator/health',
                    'POST /api/shipments/create',
                    'PUT /api/shipments/:shipmentId/status',
                    'GET /api/shipments/:shipmentId'
                ]
            },
            order: {
                baseUrl: 'http://localhost:8080',
                endpoints: [
                    'GET /actuator/health',
                    'POST /api/orders/process',
                    'GET /api/orders/:orderId',
                    'GET /api/orders',
                    'GET /api/orders/customer/:customerId'
                ]
            }
        }
    });
});

app.listen(PORT, () => {
    console.log(`🚀 API Tester Server Started`);
    console.log(`📊 Web Interface: http://localhost:${PORT}`);
    console.log(`📝 API Documentation: http://localhost:${PORT}/api/docs`);
    console.log(`🛑 Press Ctrl+C to stop`);
});

module.exports = app;