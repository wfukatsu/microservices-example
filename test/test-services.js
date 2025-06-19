#!/usr/bin/env node

/**
 * Comprehensive Test Suite for Microservices
 * Tests all API endpoints and integration scenarios
 */

const axios = require('axios');
const { v4: uuidv4 } = require('uuid');

// Configuration
const BASE_URLS = {
  inventory: 'http://localhost:8081',
  payment: 'http://localhost:8082',
  shipping: 'http://localhost:8083',
  order: 'http://localhost:8080'
};

// Test utilities
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

const colors = {
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  reset: '\x1b[0m'
};

const log = {
  info: (msg) => console.log(`${colors.blue}[INFO]${colors.reset} ${msg}`),
  success: (msg) => console.log(`${colors.green}[PASS]${colors.reset} ${msg}`),
  error: (msg) => console.log(`${colors.red}[FAIL]${colors.reset} ${msg}`),
  warning: (msg) => console.log(`${colors.yellow}[WARN]${colors.reset} ${msg}`)
};

// Test results
let totalTests = 0;
let passedTests = 0;
let failedTests = 0;

function assert(condition, message) {
  totalTests++;
  if (condition) {
    passedTests++;
    log.success(message);
  } else {
    failedTests++;
    log.error(message);
  }
}

async function makeRequest(method, url, data = null) {
  try {
    const config = { method, url };
    if (data) config.data = data;
    const response = await axios(config);
    return { success: true, data: response.data, status: response.status };
  } catch (error) {
    return {
      success: false,
      error: error.response?.data || error.message,
      status: error.response?.status || 500
    };
  }
}

// Test functions
async function testHealthEndpoints() {
  log.info('Testing health endpoints...');
  
  for (const [service, baseUrl] of Object.entries(BASE_URLS)) {
    const result = await makeRequest('GET', `${baseUrl}/actuator/health`);
    assert(
      result.success && result.data.status === 'UP',
      `${service} service health check`
    );
  }
}

async function testInventoryService() {
  log.info('Testing Inventory Service...');
  const baseUrl = BASE_URLS.inventory;
  
  // Test get all inventory items
  let result = await makeRequest('GET', `${baseUrl}/api/inventory`);
  assert(
    result.success && Array.isArray(result.data),
    'Get all inventory items'
  );
  
  const items = result.data;
  if (items.length > 0) {
    const productId = items[0].productId;
    
    // Test get specific item
    result = await makeRequest('GET', `${baseUrl}/api/inventory/${productId}`);
    assert(
      result.success && result.data.productId === productId,
      `Get specific inventory item: ${productId}`
    );
    
    // Test update inventory
    const originalQuantity = result.data.quantity;
    const updateData = { quantity: originalQuantity + 10 };
    result = await makeRequest('PUT', `${baseUrl}/api/inventory/${productId}`, updateData);
    assert(
      result.success && result.data.quantity === originalQuantity + 10,
      'Update inventory quantity'
    );
    
    // Test inventory reservation
    const reservationData = {
      orderId: uuidv4(),
      customerId: 'CUST001',
      items: [{ productId, quantity: 2 }]
    };
    
    result = await makeRequest('POST', `${baseUrl}/api/inventory/reserve`, reservationData);
    assert(
      result.success && result.data.reservationId,
      'Create inventory reservation'
    );
    
    if (result.success) {
      const reservationId = result.data.reservationId;
      
      // Test confirm reservation
      result = await makeRequest('PUT', `${baseUrl}/api/inventory/reservation/${reservationId}/confirm`);
      assert(
        result.success && result.data.status === 'CONFIRMED',
        'Confirm inventory reservation'
      );
    }
  }
  
  // Test category filter
  result = await makeRequest('GET', `${baseUrl}/api/inventory?category=Electronics`);
  assert(
    result.success && Array.isArray(result.data),
    'Get inventory items by category'
  );
}

async function testPaymentService() {
  log.info('Testing Payment Service...');
  const baseUrl = BASE_URLS.payment;
  
  // Test process payment
  const paymentData = {
    orderId: uuidv4(),
    customerId: 'CUST001',
    amount: 50000,
    paymentMethod: 'CREDIT_CARD',
    currency: 'JPY'
  };
  
  let result = await makeRequest('POST', `${baseUrl}/api/payments/process`, paymentData);
  assert(
    result.success && result.data.paymentId,
    'Process payment'
  );
  
  if (result.success) {
    const payment = result.data;
    
    // Test get payments by customer
    result = await makeRequest('GET', `${baseUrl}/api/payments/customer/${payment.customerId}`);
    assert(
      result.success && Array.isArray(result.data),
      'Get payments by customer'
    );
    
    // Test refund (only if payment was successful)
    if (payment.status === 'COMPLETED') {
      const refundData = {
        amount: payment.amount / 2,
        reason: 'Partial refund for testing'
      };
      
      result = await makeRequest('POST', `${baseUrl}/api/payments/${payment.paymentId}/refund`, refundData);
      assert(
        result.success && result.data.refundId,
        'Process payment refund'
      );
    }
  }
}

async function testShippingService() {
  log.info('Testing Shipping Service...');
  const baseUrl = BASE_URLS.shipping;
  
  // Test create shipment
  const shipmentData = {
    orderId: uuidv4(),
    customerId: 'CUST001',
    shippingAddress: {
      name: '田中太郎',
      postalCode: '100-0001',
      prefecture: '東京都',
      city: '千代田区',
      address: '千代田1-1-1',
      phone: '03-1234-5678'
    },
    items: [
      { productId: 'ITEM001', productName: 'ノートパソコン', quantity: 1, weight: 2.5 }
    ]
  };
  
  let result = await makeRequest('POST', `${baseUrl}/api/shipments/create`, shipmentData);
  assert(
    result.success && result.data.shipmentId,
    'Create shipment'
  );
  
  if (result.success) {
    const shipment = result.data;
    
    // Test get shipment
    result = await makeRequest('GET', `${baseUrl}/api/shipments/${shipment.shipmentId}`);
    assert(
      result.success && result.data.shipmentId === shipment.shipmentId,
      'Get shipment details'
    );
    
    // Test update shipment status
    const statusData = { status: 'SHIPPED' };
    result = await makeRequest('PUT', `${baseUrl}/api/shipments/${shipment.shipmentId}/status`, statusData);
    assert(
      result.success && result.data.status === 'SHIPPED',
      'Update shipment status'
    );
  }
}

async function testOrderService() {
  log.info('Testing Order Service...');
  const baseUrl = BASE_URLS.order;
  
  // Test process order
  const orderData = {
    customerId: 'CUST001',
    items: [
      { productId: 'ITEM001', quantity: 1 },
      { productId: 'ITEM002', quantity: 2 }
    ],
    shippingAddress: {
      name: '田中太郎',
      postalCode: '100-0001',
      prefecture: '東京都',
      city: '千代田区',
      address: '千代田1-1-1',
      phone: '03-1234-5678'
    },
    paymentMethod: 'CREDIT_CARD'
  };
  
  let result = await makeRequest('POST', `${baseUrl}/api/orders/process`, orderData);
  assert(
    result.success && result.data.orderId,
    'Process order'
  );
  
  if (result.success) {
    const order = result.data;
    
    // Wait a moment for order processing
    await sleep(1000);
    
    // Test get order
    result = await makeRequest('GET', `${baseUrl}/api/orders/${order.orderId}`);
    assert(
      result.success && result.data.orderId === order.orderId,
      'Get order details'
    );
    
    // Test get orders by customer
    result = await makeRequest('GET', `${baseUrl}/api/orders/customer/${order.customerId}`);
    assert(
      result.success && Array.isArray(result.data),
      'Get orders by customer'
    );
  }
}

async function testIntegrationScenario() {
  log.info('Testing end-to-end integration scenario...');
  
  const customerId = 'CUST_INTEGRATION_TEST';
  const testItems = [
    { productId: 'ITEM001', quantity: 1 },
    { productId: 'ITEM002', quantity: 1 }
  ];
  
  // 1. Check inventory availability
  let inventoryOk = true;
  for (const item of testItems) {
    const result = await makeRequest('GET', `${BASE_URLS.inventory}/api/inventory/${item.productId}`);
    if (!result.success || result.data.availableQuantity < item.quantity) {
      inventoryOk = false;
      break;
    }
  }
  
  assert(inventoryOk, 'Integration test: Check inventory availability');
  
  if (!inventoryOk) return;
  
  // 2. Reserve inventory
  const reservationData = {
    orderId: uuidv4(),
    customerId,
    items: testItems
  };
  
  let result = await makeRequest('POST', `${BASE_URLS.inventory}/api/inventory/reserve`, reservationData);
  assert(
    result.success && result.data.reservationId,
    'Integration test: Reserve inventory'
  );
  
  if (!result.success) return;
  const reservationId = result.data.reservationId;
  
  // 3. Process payment
  const paymentData = {
    orderId: reservationData.orderId,
    customerId,
    amount: 140000, // Estimated total
    paymentMethod: 'CREDIT_CARD'
  };
  
  result = await makeRequest('POST', `${BASE_URLS.payment}/api/payments/process`, paymentData);
  assert(
    result.success && result.data.status === 'COMPLETED',
    'Integration test: Process payment'
  );
  
  if (!result.success || result.data.status !== 'COMPLETED') {
    // Cancel reservation if payment failed
    await makeRequest('DELETE', `${BASE_URLS.inventory}/api/inventory/reservation/${reservationId}`);
    return;
  }
  
  // 4. Confirm reservation
  result = await makeRequest('PUT', `${BASE_URLS.inventory}/api/inventory/reservation/${reservationId}/confirm`);
  assert(
    result.success && result.data.status === 'CONFIRMED',
    'Integration test: Confirm inventory reservation'
  );
  
  // 5. Create shipment
  const shipmentData = {
    orderId: reservationData.orderId,
    customerId,
    shippingAddress: {
      name: '統合テスト',
      postalCode: '100-0001',
      prefecture: '東京都',
      city: '千代田区',
      address: '千代田1-1-1',
      phone: '03-1234-5678'
    },
    items: testItems.map(item => ({
      productId: item.productId,
      quantity: item.quantity,
      weight: 1.0
    }))
  };
  
  result = await makeRequest('POST', `${BASE_URLS.shipping}/api/shipments/create`, shipmentData);
  assert(
    result.success && result.data.shipmentId,
    'Integration test: Create shipment'
  );
  
  log.success('Integration test scenario completed successfully');
}

async function testErrorScenarios() {
  log.info('Testing error scenarios...');
  
  // Test 404 errors
  let result = await makeRequest('GET', `${BASE_URLS.inventory}/api/inventory/NONEXISTENT`);
  assert(
    !result.success && result.status === 404,
    'Handle 404 error for non-existent product'
  );
  
  // Test insufficient inventory
  const reservationData = {
    orderId: uuidv4(),
    customerId: 'CUST001',
    items: [{ productId: 'ITEM001', quantity: 999999 }]
  };
  
  result = await makeRequest('POST', `${BASE_URLS.inventory}/api/inventory/reserve`, reservationData);
  assert(
    !result.success && result.status === 400,
    'Handle insufficient inventory error'
  );
  
  // Test invalid payment refund
  result = await makeRequest('POST', `${BASE_URLS.payment}/api/payments/NONEXISTENT/refund`, {
    amount: 1000,
    reason: 'Test refund'
  });
  assert(
    !result.success && result.status === 404,
    'Handle refund for non-existent payment'
  );
}

async function runAllTests() {
  console.log('🧪 Starting Microservices Test Suite\n');
  
  try {
    await testHealthEndpoints();
    await sleep(500);
    
    await testInventoryService();
    await sleep(500);
    
    await testPaymentService();
    await sleep(500);
    
    await testShippingService();
    await sleep(500);
    
    await testOrderService();
    await sleep(500);
    
    await testIntegrationScenario();
    await sleep(500);
    
    await testErrorScenarios();
    
  } catch (error) {
    log.error(`Test execution failed: ${error.message}`);
    failedTests++;
  }
  
  // Print results
  console.log('\n📊 Test Results:');
  console.log(`   Total Tests: ${totalTests}`);
  console.log(`   ${colors.green}Passed: ${passedTests}${colors.reset}`);
  console.log(`   ${colors.red}Failed: ${failedTests}${colors.reset}`);
  console.log(`   Success Rate: ${((passedTests / totalTests) * 100).toFixed(1)}%`);
  
  if (failedTests === 0) {
    console.log(`\n${colors.green}🎉 All tests passed!${colors.reset}`);
    process.exit(0);
  } else {
    console.log(`\n${colors.red}❌ Some tests failed${colors.reset}`);
    process.exit(1);
  }
}

// Check if services are running before starting tests
async function checkServices() {
  log.info('Checking if services are running...');
  
  for (const [service, baseUrl] of Object.entries(BASE_URLS)) {
    const result = await makeRequest('GET', `${baseUrl}/actuator/health`);
    if (!result.success) {
      log.error(`${service} service is not running on ${baseUrl}`);
      log.info('Please start the mock server first: npm start');
      process.exit(1);
    }
  }
  
  log.success('All services are running');
}

// Main execution
if (require.main === module) {
  checkServices().then(runAllTests);
}

module.exports = {
  runAllTests,
  testHealthEndpoints,
  testInventoryService,
  testPaymentService,
  testShippingService,
  testOrderService
};