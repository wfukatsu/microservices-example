#!/usr/bin/env node

/**
 * Mock API Server for Testing Microservices
 * Simulates all backend services for integration testing
 */

const express = require('express');
const cors = require('cors');
const { v4: uuidv4 } = require('uuid');

// Create Express apps for each service
const inventoryApp = express();
const paymentApp = express();
const shippingApp = express();
const orderApp = express();

// Configure middleware
[inventoryApp, paymentApp, shippingApp, orderApp].forEach(app => {
  app.use(cors());
  app.use(express.json());
  app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} ${req.method} ${req.originalUrl}`);
    next();
  });
});

// Mock data stores
const inventoryItems = new Map([
  ['ITEM001', {
    productId: 'ITEM001',
    productName: 'ノートパソコン',
    category: 'Electronics',
    quantity: 50,
    reservedQuantity: 5,
    availableQuantity: 45,
    unitPrice: 80000,
    lowStockThreshold: 10,
    lastUpdated: new Date().toISOString()
  }],
  ['ITEM002', {
    productId: 'ITEM002',
    productName: 'スマートフォン',
    category: 'Electronics',
    quantity: 30,
    reservedQuantity: 2,
    availableQuantity: 28,
    unitPrice: 60000,
    lowStockThreshold: 10,
    lastUpdated: new Date().toISOString()
  }],
  ['ITEM003', {
    productId: 'ITEM003',
    productName: 'マウス',
    category: 'Accessories',
    quantity: 8,
    reservedQuantity: 1,
    availableQuantity: 7,
    unitPrice: 2500,
    lowStockThreshold: 10,
    lastUpdated: new Date().toISOString()
  }]
]);

const reservations = new Map();
const payments = new Map();
const shipments = new Map();
const orders = new Map();

// Inventory Service (Port 8081)
inventoryApp.get('/actuator/health', (req, res) => {
  res.json({ status: 'UP', service: 'inventory-service' });
});

inventoryApp.get('/api/inventory', (req, res) => {
  const items = Array.from(inventoryItems.values());
  const { category } = req.query;
  
  if (category) {
    const filtered = items.filter(item => item.category === category);
    return res.json(filtered);
  }
  
  res.json(items);
});

inventoryApp.get('/api/inventory/:productId', (req, res) => {
  const item = inventoryItems.get(req.params.productId);
  if (!item) {
    return res.status(404).json({
      error: 'NOT_FOUND',
      message: 'Product not found'
    });
  }
  res.json(item);
});

inventoryApp.put('/api/inventory/:productId', (req, res) => {
  const { productId } = req.params;
  const updates = req.body;
  
  // Input validation
  if (updates.quantity !== undefined && updates.quantity < 0) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: 'Quantity cannot be negative'
    });
  }
  
  if (updates.reservedQuantity !== undefined && updates.reservedQuantity < 0) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: 'Reserved quantity cannot be negative'
    });
  }
  
  const item = inventoryItems.get(productId);
  if (!item) {
    return res.status(404).json({
      error: 'NOT_FOUND',
      message: 'Product not found'
    });
  }
  
  const updatedItem = {
    ...item,
    ...updates,
    lastUpdated: new Date().toISOString()
  };
  
  // Recalculate available quantity
  updatedItem.availableQuantity = updatedItem.quantity - updatedItem.reservedQuantity;
  
  inventoryItems.set(productId, updatedItem);
  res.json(updatedItem);
});

inventoryApp.post('/api/inventory/reserve', (req, res) => {
  const { orderId, customerId, items, productId, quantity } = req.body;
  const reservationId = uuidv4();
  
  // Handle both array format and single item format
  const itemsToReserve = items || [{ productId, quantity }];
  
  // Check availability
  for (const item of itemsToReserve) {
    const inventory = inventoryItems.get(item.productId);
    if (!inventory || inventory.availableQuantity < item.quantity) {
      return res.status(400).json({
        message: `Insufficient inventory for product ${item.productId}`
      });
    }
  }
  
  // Create reservation
  const reservation = {
    reservationId,
    orderId,
    customerId,
    items: itemsToReserve.map(item => {
      const inventory = inventoryItems.get(item.productId);
      return {
        productId: item.productId,
        productName: inventory.productName,
        reservedQuantity: item.quantity,
        unitPrice: inventory.unitPrice
      };
    }),
    status: 'RESERVED',
    expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
    createdAt: new Date().toISOString()
  };
  
  // Update inventory
  itemsToReserve.forEach(item => {
    const inventory = inventoryItems.get(item.productId);
    inventory.reservedQuantity += item.quantity;
    inventory.availableQuantity -= item.quantity;
    inventory.lastUpdated = new Date().toISOString();
  });
  
  reservations.set(reservationId, reservation);
  res.status(201).json(reservation);
});

inventoryApp.put('/api/inventory/reservation/:reservationId/confirm', (req, res) => {
  const reservation = reservations.get(req.params.reservationId);
  if (!reservation) {
    return res.status(404).json({ message: 'Reservation not found' });
  }
  
  if (reservation.status !== 'RESERVED') {
    return res.status(400).json({ message: 'Reservation cannot be confirmed' });
  }
  
  // Update reservation status
  reservation.status = 'CONFIRMED';
  reservation.confirmedAt = new Date().toISOString();
  
  // Update inventory (reduce actual quantity)
  reservation.items.forEach(item => {
    const inventory = inventoryItems.get(item.productId);
    inventory.quantity -= item.reservedQuantity;
    inventory.reservedQuantity -= item.reservedQuantity;
    inventory.lastUpdated = new Date().toISOString();
  });
  
  res.json(reservation);
});

inventoryApp.delete('/api/inventory/reservation/:reservationId', (req, res) => {
  const reservation = reservations.get(req.params.reservationId);
  if (!reservation) {
    return res.status(404).json({ message: 'Reservation not found' });
  }
  
  if (reservation.status === 'RESERVED') {
    // Release reserved inventory
    reservation.items.forEach(item => {
      const inventory = inventoryItems.get(item.productId);
      inventory.reservedQuantity -= item.reservedQuantity;
      inventory.availableQuantity += item.reservedQuantity;
      inventory.lastUpdated = new Date().toISOString();
    });
  }
  
  reservation.status = 'CANCELLED';
  reservation.cancelledAt = new Date().toISOString();
  
  res.json({ message: 'Reservation cancelled' });
});

// Payment Service (Port 8082)
paymentApp.get('/actuator/health', (req, res) => {
  res.json({ status: 'UP', service: 'payment-service' });
});

paymentApp.post('/api/payments/process', (req, res) => {
  const { orderId, customerId, amount, paymentMethod, currency = 'JPY' } = req.body;
  
  // Input validation
  if (!orderId || !customerId || !amount || !paymentMethod) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: 'Missing required fields: orderId, customerId, amount, paymentMethod'
    });
  }
  
  if (amount <= 0) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: 'Amount must be greater than 0'
    });
  }
  
  if (!['CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER', 'DIGITAL_WALLET'].includes(paymentMethod)) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: 'Invalid payment method'
    });
  }
  
  const paymentId = uuidv4();
  
  // Process payment synchronously to avoid race conditions
  const isSuccessful = Math.random() > 0.1; // 90% success rate
  const payment = {
    paymentId,
    orderId,
    customerId,
    amount,
    currency,
    paymentMethod,
    status: isSuccessful ? 'COMPLETED' : 'FAILED',
    transactionId: `TXN_${Date.now()}`,
    processedAt: new Date().toISOString(),
    providerResponse: {
      code: isSuccessful ? '200' : '400',
      message: isSuccessful ? 'Payment processed successfully' : 'Payment failed'
    }
  };
  
  payments.set(paymentId, payment);
  
  if (isSuccessful) {
    res.status(201).json(payment);
  } else {
    res.status(400).json({
      error: 'PAYMENT_FAILED',
      message: 'Payment processing failed',
      payment
    });
  }
});

paymentApp.post('/api/payments/:paymentId/refund', (req, res) => {
  const { paymentId } = req.params;
  const { amount, reason } = req.body;
  
  // Input validation
  if (!reason) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: 'Refund reason is required'
    });
  }
  
  const payment = payments.get(paymentId);
  if (!payment) {
    return res.status(404).json({
      error: 'NOT_FOUND',
      message: 'Payment not found'
    });
  }
  
  if (payment.status !== 'COMPLETED') {
    return res.status(400).json({
      error: 'INVALID_STATUS',
      message: 'Payment cannot be refunded. Current status: ' + payment.status
    });
  }
  
  const refundAmount = amount || payment.amount;
  if (refundAmount <= 0 || refundAmount > payment.amount) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: 'Invalid refund amount. Must be between 0 and ' + payment.amount
    });
  }
  
  const refundId = uuidv4();
  const refund = {
    refundId,
    paymentId,
    amount: refundAmount,
    reason,
    status: 'COMPLETED',
    processedAt: new Date().toISOString(),
    transactionId: `REF_${Date.now()}`
  };
  
  // Update payment status
  payment.status = 'REFUNDED';
  payment.refundedAt = new Date().toISOString();
  
  res.status(201).json(refund);
});

paymentApp.get('/api/payments/customer/:customerId', (req, res) => {
  const customerPayments = Array.from(payments.values())
    .filter(p => p.customerId === req.params.customerId);
  res.json(customerPayments);
});

// Shipping Service (Port 8083)
shippingApp.get('/actuator/health', (req, res) => {
  res.json({ status: 'UP', service: 'shipping-service' });
});

shippingApp.post('/api/shipments/create', (req, res) => {
  const { orderId, customerId, shippingAddress, items } = req.body;
  
  // Input validation
  if (!orderId || !customerId || !shippingAddress || !items) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: 'Missing required fields: orderId, customerId, shippingAddress, items'
    });
  }
  
  if (!Array.isArray(items) || items.length === 0) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: 'Items must be a non-empty array'
    });
  }
  
  // Validate shipping address
  const requiredAddressFields = ['name', 'postalCode', 'prefecture', 'city', 'address'];
  for (const field of requiredAddressFields) {
    if (!shippingAddress[field]) {
      return res.status(400).json({
        error: 'VALIDATION_ERROR',
        message: `Missing required address field: ${field}`
      });
    }
  }
  
  // Validate items
  for (const item of items) {
    if (!item.productId || !item.quantity || item.quantity <= 0) {
      return res.status(400).json({
        error: 'VALIDATION_ERROR',
        message: 'Each item must have productId and quantity > 0'
      });
    }
  }
  
  const shipmentId = uuidv4();
  
  const shipment = {
    shipmentId,
    orderId,
    customerId,
    shippingAddress,
    items: items.map(item => ({
      productId: item.productId,
      productName: item.productName || `Product ${item.productId}`,
      quantity: item.quantity,
      weight: item.weight || 1.0
    })),
    status: 'PENDING',
    trackingNumber: `TRK${Date.now()}`,
    carrier: 'ヤマト運輸',
    estimatedDelivery: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString(),
    createdAt: new Date().toISOString()
  };
  
  shipments.set(shipmentId, shipment);
  res.status(201).json(shipment);
});

shippingApp.put('/api/shipments/:shipmentId/status', (req, res) => {
  const { shipmentId } = req.params;
  const { status } = req.body;
  
  // Input validation
  if (!status) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: 'Status is required'
    });
  }
  
  const validStatuses = ['PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'];
  if (!validStatuses.includes(status)) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: `Invalid status. Valid values: ${validStatuses.join(', ')}`
    });
  }
  
  const shipment = shipments.get(shipmentId);
  if (!shipment) {
    return res.status(404).json({
      error: 'NOT_FOUND',
      message: 'Shipment not found'
    });
  }
  
  // Validate status transitions
  const currentStatus = shipment.status;
  const invalidTransitions = {
    'DELIVERED': ['PENDING', 'PROCESSING', 'SHIPPED'],
    'CANCELLED': ['SHIPPED', 'DELIVERED']
  };
  
  if (invalidTransitions[currentStatus] && invalidTransitions[currentStatus].includes(status)) {
    return res.status(400).json({
      error: 'INVALID_TRANSITION',
      message: `Cannot change status from ${currentStatus} to ${status}`
    });
  }
  
  shipment.status = status;
  shipment.lastUpdated = new Date().toISOString();
  
  if (status === 'SHIPPED') {
    shipment.shippedAt = new Date().toISOString();
  } else if (status === 'DELIVERED') {
    shipment.deliveredAt = new Date().toISOString();
  }
  
  res.json(shipment);
});

shippingApp.get('/api/shipments/:shipmentId', (req, res) => {
  const shipment = shipments.get(req.params.shipmentId);
  if (!shipment) {
    return res.status(404).json({ message: 'Shipment not found' });
  }
  res.json(shipment);
});

// Order Service (Port 8080)
orderApp.get('/actuator/health', (req, res) => {
  res.json({ status: 'UP', service: 'order-service' });
});

orderApp.post('/api/orders/process', (req, res) => {
  const { customerId, items, shippingAddress, paymentMethod } = req.body;
  
  // Input validation
  if (!customerId || !items || !shippingAddress || !paymentMethod) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: 'Missing required fields: customerId, items, shippingAddress, paymentMethod'
    });
  }
  
  if (!Array.isArray(items) || items.length === 0) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: 'Items must be a non-empty array'
    });
  }
  
  // Validate items and calculate total amount
  let totalAmount = 0;
  for (const item of items) {
    if (!item.productId || !item.quantity || item.quantity <= 0) {
      return res.status(400).json({
        error: 'VALIDATION_ERROR',
        message: 'Each item must have productId and quantity > 0'
      });
    }
    
    const inventory = inventoryItems.get(item.productId);
    if (!inventory) {
      return res.status(400).json({
        error: 'PRODUCT_NOT_FOUND',
        message: `Product ${item.productId} not found`
      });
    }
    
    if (inventory.availableQuantity < item.quantity) {
      return res.status(400).json({
        error: 'INSUFFICIENT_INVENTORY',
        message: `Insufficient inventory for product ${item.productId}. Available: ${inventory.availableQuantity}, Requested: ${item.quantity}`
      });
    }
    
    totalAmount += inventory.unitPrice * item.quantity;
  }
  
  const orderId = uuidv4();
  
  // Process order synchronously to avoid race conditions
  const order = {
    orderId,
    customerId,
    items,
    totalAmount,
    shippingAddress,
    paymentMethod,
    status: 'PROCESSING',
    createdAt: new Date().toISOString(),
    steps: {
      inventory: { status: 'PENDING', message: 'Reserving inventory...' },
      payment: { status: 'PENDING', message: 'Processing payment...' },
      shipping: { status: 'PENDING', message: 'Creating shipment...' }
    }
  };
  
  orders.set(orderId, order);
  res.status(201).json(order);
});

orderApp.get('/api/orders/:orderId', (req, res) => {
  const order = orders.get(req.params.orderId);
  if (!order) {
    return res.status(404).json({
      error: 'NOT_FOUND',
      message: 'Order not found'
    });
  }
  res.json(order);
});

// Get all orders (with pagination)
orderApp.get('/api/orders', (req, res) => {
  const page = parseInt(req.query.page) || 1;
  const limit = parseInt(req.query.limit) || 10;
  const offset = (page - 1) * limit;
  
  const allOrders = Array.from(orders.values())
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  
  const paginatedOrders = allOrders.slice(offset, offset + limit);
  
  res.json({
    orders: paginatedOrders,
    pagination: {
      page,
      limit,
      total: allOrders.length,
      totalPages: Math.ceil(allOrders.length / limit)
    }
  });
});

orderApp.get('/api/orders/customer/:customerId', (req, res) => {
  const customerOrders = Array.from(orders.values())
    .filter(o => o.customerId === req.params.customerId);
  res.json(customerOrders);
});

// === DEMO APPLICATION ENDPOINTS ===

// Wallet Management for Demo
const wallets = new Map();
// Initialize demo wallets
wallets.set('DEMO-USER-001', { balance: 500000, currency: 'JPY', lastUpdated: new Date().toISOString() });
wallets.set('DEMO-USER-002', { balance: 300000, currency: 'JPY', lastUpdated: new Date().toISOString() });

// Wallet endpoints
paymentApp.get('/api/wallet/:customerId', (req, res) => {
  const { customerId } = req.params;
  const wallet = wallets.get(customerId);
  
  if (!wallet) {
    return res.status(404).json({
      error: 'NOT_FOUND',
      message: 'Wallet not found'
    });
  }
  
  res.json({
    customerId,
    balance: wallet.balance,
    currency: wallet.currency,
    lastUpdated: wallet.lastUpdated
  });
});

paymentApp.post('/api/wallet/:customerId/add-funds', (req, res) => {
  const { customerId } = req.params;
  const { amount, source = 'BANK_TRANSFER' } = req.body;
  
  if (!amount || amount <= 0) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: 'Amount must be greater than 0'
    });
  }
  
  let wallet = wallets.get(customerId);
  if (!wallet) {
    wallet = { balance: 0, currency: 'JPY', lastUpdated: new Date().toISOString() };
  }
  
  wallet.balance += amount;
  wallet.lastUpdated = new Date().toISOString();
  wallets.set(customerId, wallet);
  
  res.json({
    customerId,
    balance: wallet.balance,
    currency: wallet.currency,
    addedAmount: amount,
    source,
    transactionId: `FUND_${Date.now()}`,
    processedAt: wallet.lastUpdated
  });
});

// Enhanced payment processing with wallet support
paymentApp.post('/api/payments/process-with-wallet', (req, res) => {
  const { orderId, customerId, amount, currency = 'JPY' } = req.body;
  
  if (!orderId || !customerId || !amount) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: 'Missing required fields: orderId, customerId, amount'
    });
  }
  
  const wallet = wallets.get(customerId);
  if (!wallet) {
    return res.status(404).json({
      error: 'WALLET_NOT_FOUND',
      message: 'Customer wallet not found'
    });
  }
  
  if (wallet.balance < amount) {
    return res.status(400).json({
      error: 'INSUFFICIENT_FUNDS',
      message: `Insufficient wallet balance. Available: ${wallet.balance}, Required: ${amount}`
    });
  }
  
  // Deduct from wallet
  wallet.balance -= amount;
  wallet.lastUpdated = new Date().toISOString();
  wallets.set(customerId, wallet);
  
  const paymentId = uuidv4();
  const payment = {
    paymentId,
    orderId,
    customerId,
    amount,
    currency,
    paymentMethod: 'WALLET',
    status: 'COMPLETED',
    transactionId: `TXN_${Date.now()}`,
    processedAt: new Date().toISOString(),
    walletBalanceAfter: wallet.balance,
    providerResponse: {
      code: '200',
      message: 'Payment processed from wallet successfully'
    }
  };
  
  payments.set(paymentId, payment);
  res.status(201).json(payment);
});

// Inventory restocking endpoint
inventoryApp.post('/api/inventory/:productId/restock', (req, res) => {
  const { productId } = req.params;
  const { quantity, supplier = 'DEFAULT_SUPPLIER', cost } = req.body;
  
  if (!quantity || quantity <= 0) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: 'Quantity must be greater than 0'
    });
  }
  
  const item = inventoryItems.get(productId);
  if (!item) {
    return res.status(404).json({
      error: 'NOT_FOUND',
      message: 'Product not found'
    });
  }
  
  // Update inventory
  item.quantity += quantity;
  item.availableQuantity = item.quantity - item.reservedQuantity;
  item.lastUpdated = new Date().toISOString();
  
  inventoryItems.set(productId, item);
  
  const restockId = uuidv4();
  const restockRecord = {
    restockId,
    productId,
    productName: item.productName,
    quantity,
    supplier,
    cost: cost || 0,
    newTotalQuantity: item.quantity,
    processedAt: new Date().toISOString()
  };
  
  res.status(201).json(restockRecord);
});

// Enhanced order processing for demo
orderApp.post('/api/orders/demo-process', (req, res) => {
  const { customerId, items, shippingAddress, useWallet = false } = req.body;
  
  if (!customerId || !items || !shippingAddress) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: 'Missing required fields: customerId, items, shippingAddress'
    });
  }
  
  if (!Array.isArray(items) || items.length === 0) {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: 'Items must be a non-empty array'
    });
  }
  
  // Calculate total and validate inventory
  let totalAmount = 0;
  for (const item of items) {
    if (!item.productId || !item.quantity || item.quantity <= 0) {
      return res.status(400).json({
        error: 'VALIDATION_ERROR',
        message: 'Each item must have productId and quantity > 0'
      });
    }
    
    const inventory = inventoryItems.get(item.productId);
    if (!inventory) {
      return res.status(400).json({
        error: 'PRODUCT_NOT_FOUND',
        message: `Product ${item.productId} not found`
      });
    }
    
    if (inventory.availableQuantity < item.quantity) {
      return res.status(400).json({
        error: 'INSUFFICIENT_INVENTORY',
        message: `Insufficient inventory for product ${item.productId}. Available: ${inventory.availableQuantity}, Requested: ${item.quantity}`
      });
    }
    
    totalAmount += inventory.unitPrice * item.quantity;
  }
  
  // Check wallet balance if using wallet
  if (useWallet) {
    const wallet = wallets.get(customerId);
    if (!wallet) {
      return res.status(404).json({
        error: 'WALLET_NOT_FOUND',
        message: 'Customer wallet not found'
      });
    }
    
    if (wallet.balance < totalAmount) {
      return res.status(400).json({
        error: 'INSUFFICIENT_FUNDS',
        message: `Insufficient wallet balance. Available: ${wallet.balance}, Required: ${totalAmount}`
      });
    }
  }
  
  const orderId = uuidv4();
  
  // Reserve inventory
  items.forEach(item => {
    const inventory = inventoryItems.get(item.productId);
    inventory.reservedQuantity += item.quantity;
    inventory.availableQuantity -= item.quantity;
    inventory.lastUpdated = new Date().toISOString();
  });
  
  // Process payment
  let paymentResult = null;
  if (useWallet) {
    const wallet = wallets.get(customerId);
    wallet.balance -= totalAmount;
    wallet.lastUpdated = new Date().toISOString();
    wallets.set(customerId, wallet);
    
    paymentResult = {
      paymentId: uuidv4(),
      paymentMethod: 'WALLET',
      status: 'COMPLETED',
      walletBalanceAfter: wallet.balance
    };
  } else {
    paymentResult = {
      paymentId: uuidv4(),
      paymentMethod: 'CREDIT_CARD',
      status: 'COMPLETED'
    };
  }
  
  // Create shipment
  const shipmentId = uuidv4();
  const shipment = {
    shipmentId,
    orderId,
    customerId,
    shippingAddress,
    items: items.map(item => ({
      productId: item.productId,
      productName: inventoryItems.get(item.productId).productName,
      quantity: item.quantity,
      weight: item.weight || 1.0
    })),
    status: 'PENDING',
    trackingNumber: `DEMO${Date.now()}`,
    carrier: 'ヤマト運輸',
    estimatedDelivery: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString(),
    createdAt: new Date().toISOString()
  };
  
  shipments.set(shipmentId, shipment);
  
  // Create order
  const order = {
    orderId,
    customerId,
    items,
    totalAmount,
    shippingAddress,
    paymentMethod: useWallet ? 'WALLET' : 'CREDIT_CARD',
    status: 'CONFIRMED',
    createdAt: new Date().toISOString(),
    payment: paymentResult,
    shipment: {
      shipmentId,
      trackingNumber: shipment.trackingNumber,
      status: shipment.status,
      estimatedDelivery: shipment.estimatedDelivery
    }
  };
  
  orders.set(orderId, order);
  
  res.status(201).json(order);
});

// Enhanced shipping tracking
shippingApp.get('/api/shipments/track/:trackingNumber', (req, res) => {
  const { trackingNumber } = req.params;
  
  const shipment = Array.from(shipments.values()).find(s => s.trackingNumber === trackingNumber);
  
  if (!shipment) {
    return res.status(404).json({
      error: 'NOT_FOUND',
      message: 'Tracking number not found'
    });
  }
  
  // Generate tracking history
  const trackingHistory = [
    {
      status: 'PENDING',
      description: '注文を受け付けました',
      timestamp: shipment.createdAt,
      location: '配送センター'
    }
  ];
  
  if (shipment.status !== 'PENDING') {
    trackingHistory.push({
      status: 'PROCESSING',
      description: '商品を準備中です',
      timestamp: new Date(new Date(shipment.createdAt).getTime() + 60 * 60 * 1000).toISOString(),
      location: '配送センター'
    });
  }
  
  if (['SHIPPED', 'DELIVERED'].includes(shipment.status)) {
    trackingHistory.push({
      status: 'SHIPPED',
      description: '商品が発送されました',
      timestamp: shipment.shippedAt || new Date(new Date(shipment.createdAt).getTime() + 2 * 60 * 60 * 1000).toISOString(),
      location: '配送センター'
    });
  }
  
  if (shipment.status === 'DELIVERED') {
    trackingHistory.push({
      status: 'DELIVERED',
      description: '配送が完了しました',
      timestamp: shipment.deliveredAt || new Date().toISOString(),
      location: shipment.shippingAddress?.city || '配送先'
    });
  }
  
  res.json({
    trackingNumber,
    currentStatus: shipment.status,
    estimatedDelivery: shipment.estimatedDelivery,
    carrier: shipment.carrier,
    trackingHistory
  });
});

// Start servers
const PORT_INVENTORY = 8081;
const PORT_PAYMENT = 8082;
const PORT_SHIPPING = 8083;
const PORT_ORDER = 8080;

const servers = [];

servers.push(inventoryApp.listen(PORT_INVENTORY, () => {
  console.log(`✅ Inventory Service running on port ${PORT_INVENTORY}`);
}));

servers.push(paymentApp.listen(PORT_PAYMENT, () => {
  console.log(`✅ Payment Service running on port ${PORT_PAYMENT}`);
}));

servers.push(shippingApp.listen(PORT_SHIPPING, () => {
  console.log(`✅ Shipping Service running on port ${PORT_SHIPPING}`);
}));

servers.push(orderApp.listen(PORT_ORDER, () => {
  console.log(`✅ Order Service running on port ${PORT_ORDER}`);
}));

console.log('\n🚀 Mock Microservices Started');
console.log('📊 Health Endpoints:');
console.log(`   Inventory: http://localhost:${PORT_INVENTORY}/actuator/health`);
console.log(`   Payment:   http://localhost:${PORT_PAYMENT}/actuator/health`);
console.log(`   Shipping:  http://localhost:${PORT_SHIPPING}/actuator/health`);
console.log(`   Order:     http://localhost:${PORT_ORDER}/actuator/health`);
console.log('\n📝 API Documentation available in the service implementations');
console.log('\n🛑 Press Ctrl+C to stop all services');

// Graceful shutdown
process.on('SIGINT', () => {
  console.log('\n🛑 Shutting down mock services...');
  servers.forEach(server => server.close());
  process.exit(0);
});

module.exports = {
  inventoryApp,
  paymentApp,
  shippingApp,
  orderApp
};