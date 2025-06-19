#!/bin/bash

# Performance Test Script for Microservices
set -e

echo "⚡ Starting Performance Tests for Microservices..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_test() {
    echo -e "${BLUE}[TEST]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RESULTS_DIR="$PROJECT_ROOT/performance-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_FILE="$RESULTS_DIR/performance_report_$TIMESTAMP.md"

# Test configuration
INVENTORY_URL="http://localhost:8081"
PAYMENT_URL="http://localhost:8082"
SHIPPING_URL="http://localhost:8083"
ORDER_URL="http://localhost:8080"

CONCURRENT_USERS=10
TEST_DURATION=60
RAMP_UP_TIME=10

# Create results directory
mkdir -p "$RESULTS_DIR"

# Function to check if service is running
check_service() {
    local service_name=$1
    local url=$2
    
    if curl -f "$url/actuator/health" >/dev/null 2>&1; then
        print_status "$service_name is running ✅"
        return 0
    else
        print_error "$service_name is not running ❌"
        return 1
    fi
}

# Function to run load test using curl
run_load_test() {
    local test_name=$1
    local url=$2
    local method=$3
    local data=$4
    local concurrent=$5
    local duration=$6
    
    print_test "Running $test_name..."
    
    local temp_file="/tmp/perf_test_$$"
    local success_count=0
    local error_count=0
    local total_time=0
    
    # Run concurrent requests
    for ((i=1; i<=concurrent; i++)); do
        {
            local start_time=$(date +%s.%N)
            
            if [ "$method" = "POST" ]; then
                response=$(curl -s -w "%{http_code}" -X POST \
                    -H "Content-Type: application/json" \
                    -d "$data" \
                    "$url" 2>/dev/null || echo "000")
            else
                response=$(curl -s -w "%{http_code}" "$url" 2>/dev/null || echo "000")
            fi
            
            local end_time=$(date +%s.%N)
            local request_time=$(echo "$end_time - $start_time" | bc -l)
            
            if [[ "$response" =~ ^[2-3][0-9][0-9]$ ]]; then
                echo "SUCCESS:$request_time" >> "$temp_file"
            else
                echo "ERROR:$request_time:$response" >> "$temp_file"
            fi
        } &
        
        # Limit concurrent processes
        if (( i % 5 == 0 )); then
            wait
        fi
    done
    
    wait # Wait for all background processes
    
    # Analyze results
    if [ -f "$temp_file" ]; then
        success_count=$(grep -c "^SUCCESS:" "$temp_file" || echo "0")
        error_count=$(grep -c "^ERROR:" "$temp_file" || echo "0")
        
        if [ "$success_count" -gt 0 ]; then
            local avg_time=$(grep "^SUCCESS:" "$temp_file" | cut -d':' -f2 | \
                awk '{ sum += $1; count++ } END { if (count > 0) print sum/count; else print 0 }')
            local min_time=$(grep "^SUCCESS:" "$temp_file" | cut -d':' -f2 | sort -n | head -1)
            local max_time=$(grep "^SUCCESS:" "$temp_file" | cut -d':' -f2 | sort -n | tail -1)
            
            echo "### $test_name" >> "$REPORT_FILE"
            echo "- Concurrent Users: $concurrent" >> "$REPORT_FILE"
            echo "- Total Requests: $((success_count + error_count))" >> "$REPORT_FILE"
            echo "- Successful Requests: $success_count" >> "$REPORT_FILE"
            echo "- Failed Requests: $error_count" >> "$REPORT_FILE"
            echo "- Success Rate: $(echo "scale=2; $success_count * 100 / ($success_count + $error_count)" | bc)%" >> "$REPORT_FILE"
            echo "- Average Response Time: ${avg_time}s" >> "$REPORT_FILE"
            echo "- Min Response Time: ${min_time}s" >> "$REPORT_FILE"
            echo "- Max Response Time: ${max_time}s" >> "$REPORT_FILE"
            echo "" >> "$REPORT_FILE"
            
            print_status "$test_name completed - Success: $success_count, Errors: $error_count, Avg Time: ${avg_time}s"
        fi
    fi
    
    rm -f "$temp_file"
}

# Function to test inventory service
test_inventory_performance() {
    print_test "Testing Inventory Service Performance..."
    
    # Test inventory check
    run_load_test "Inventory Check" \
        "$INVENTORY_URL/api/v1/inventory/check?productId=PROD-001&quantity=1" \
        "GET" "" 20 30
    
    # Test inventory reservation
    local reserve_data='{
        "orderId": "PERF-TEST-001",
        "customerId": "CUST-PERF",
        "items": [
            {
                "productId": "PROD-001",
                "quantity": 1
            }
        ]
    }'
    
    run_load_test "Inventory Reservation" \
        "$INVENTORY_URL/api/v1/inventory/reserve" \
        "POST" "$reserve_data" 10 20
}

# Function to test payment service
test_payment_performance() {
    print_test "Testing Payment Service Performance..."
    
    local payment_data='{
        "orderId": "PERF-TEST-002",
        "customerId": "CUST-PERF",
        "amount": 1000.00,
        "currency": "JPY",
        "paymentMethod": "CREDIT_CARD",
        "description": "Performance test payment",
        "paymentMethodDetails": {
            "cardNumber": "4111111111111111",
            "expiryMonth": "12",
            "expiryYear": "2025",
            "cvv": "123",
            "cardholderName": "Performance Test"
        }
    }'
    
    run_load_test "Payment Processing" \
        "$PAYMENT_URL/api/v1/payments/process" \
        "POST" "$payment_data" 8 25
}

# Function to test shipping service
test_shipping_performance() {
    print_test "Testing Shipping Service Performance..."
    
    local shipping_data='{
        "orderId": "PERF-TEST-003",
        "customerId": "CUST-PERF",
        "shippingMethod": "STANDARD",
        "carrier": "YAMATO",
        "recipientInfo": {
            "name": "Performance Test",
            "phone": "090-1234-5678",
            "address": "Test Address",
            "city": "Tokyo",
            "state": "Tokyo",
            "postalCode": "100-0001",
            "country": "JP"
        },
        "packageInfo": {
            "weight": 1.0,
            "dimensions": "10x10x10",
            "specialInstructions": "Performance test"
        },
        "items": [
            {
                "productId": "PROD-001",
                "productName": "Test Product",
                "quantity": 1,
                "weight": 1.0
            }
        ]
    }'
    
    run_load_test "Shipping Creation" \
        "$SHIPPING_URL/api/v1/shipping/shipments" \
        "POST" "$shipping_data" 6 20
}

# Function to test order service (distributed transactions)
test_order_performance() {
    print_test "Testing Order Service Performance (Distributed Transactions)..."
    
    local order_data='{
        "customerId": "CUST-PERF",
        "items": [
            {
                "productId": "PROD-001",
                "quantity": 1
            }
        ],
        "paymentMethodDetails": {
            "paymentMethod": "CREDIT_CARD",
            "cardNumber": "4111111111111111",
            "expiryMonth": "12",
            "expiryYear": "2025",
            "cvv": "123",
            "cardholderName": "Performance Test"
        },
        "shippingInfo": {
            "shippingMethod": "STANDARD",
            "carrier": "YAMATO",
            "recipientInfo": {
                "name": "Performance Test",
                "phone": "090-1234-5678",
                "address": "Test Address",
                "city": "Tokyo",
                "state": "Tokyo",
                "postalCode": "100-0001",
                "country": "JP"
            }
        },
        "notes": "Performance test order"
    }'
    
    run_load_test "Order Creation (Distributed Transaction)" \
        "$ORDER_URL/api/v1/orders" \
        "POST" "$order_data" 5 30
}

# Function to run system-wide performance test
test_system_performance() {
    print_test "Running System-wide Performance Test..."
    
    # Create test report header
    echo "# Microservices Performance Test Report" > "$REPORT_FILE"
    echo "Generated on: $(date)" >> "$REPORT_FILE"
    echo "Test Configuration:" >> "$REPORT_FILE"
    echo "- Max Concurrent Users: $CONCURRENT_USERS" >> "$REPORT_FILE"
    echo "- Test Duration: $TEST_DURATION seconds" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    # Check all services
    print_status "Checking service availability..."
    check_service "Inventory Service" "$INVENTORY_URL" || return 1
    check_service "Payment Service" "$PAYMENT_URL" || return 1
    check_service "Shipping Service" "$SHIPPING_URL" || return 1
    check_service "Order Service" "$ORDER_URL" || return 1
    
    # Run performance tests
    test_inventory_performance
    test_payment_performance
    test_shipping_performance
    test_order_performance
    
    # Generate summary
    echo "## Summary" >> "$REPORT_FILE"
    echo "Performance testing completed successfully." >> "$REPORT_FILE"
    echo "Results show the system's behavior under concurrent load." >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "### Recommendations" >> "$REPORT_FILE"
    echo "- Monitor response times under production load" >> "$REPORT_FILE"
    echo "- Implement caching for frequently accessed data" >> "$REPORT_FILE"
    echo "- Consider connection pooling optimization" >> "$REPORT_FILE"
    echo "- Set up proper monitoring and alerting" >> "$REPORT_FILE"
    
    print_status "Performance test completed! Report saved to: $REPORT_FILE"
}

# Function to monitor system resources during test
monitor_resources() {
    print_status "Monitoring system resources..."
    
    local monitor_file="$RESULTS_DIR/resource_usage_$TIMESTAMP.log"
    
    # Monitor CPU and memory usage
    {
        echo "Timestamp,CPU%,Memory%,DiskIO"
        while true; do
            local cpu=$(top -l 1 | grep "CPU usage" | awk '{print $3}' | tr -d '%' 2>/dev/null || echo "0")
            local memory=$(vm_stat | grep "Pages active" | awk '{print $3}' | tr -d '.' 2>/dev/null || echo "0")
            local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
            
            echo "$timestamp,$cpu,$memory,0"
            sleep 5
        done
    } > "$monitor_file" &
    
    local monitor_pid=$!
    
    # Stop monitoring after test duration
    sleep $((TEST_DURATION + 30))
    kill $monitor_pid 2>/dev/null || true
    
    print_status "Resource monitoring saved to: $monitor_file"
}

# Main execution
main() {
    print_status "Starting microservices performance test suite..."
    
    # Start resource monitoring in background
    monitor_resources &
    local monitor_bg_pid=$!
    
    # Run system performance tests
    if test_system_performance; then
        print_status "🎉 Performance testing completed successfully!"
        
        # Display summary
        if [ -f "$REPORT_FILE" ]; then
            echo ""
            echo "📊 Performance Test Summary:"
            grep -E "^- (Total|Successful|Failed|Success Rate|Average Response Time):" "$REPORT_FILE" | head -20
        fi
        
        exit 0
    else
        print_error "❌ Performance testing failed!"
        exit 1
    fi
}

# Cleanup function
cleanup() {
    # Kill any background processes
    jobs -p | xargs -r kill 2>/dev/null || true
}

trap cleanup EXIT

# Check if script is being run directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi