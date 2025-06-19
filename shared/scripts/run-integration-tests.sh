#!/bin/bash

# Integration Test Runner Script for Microservices
set -e

echo "🚀 Starting Integration Tests for Microservices..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SERVICES=("inventory-service" "payment-service" "shipping-service" "order-service")
TEST_PROFILE="test"

# Clean up function
cleanup() {
    print_status "Cleaning up test environment..."
    
    # Stop any running services
    for service in "${SERVICES[@]}"; do
        pkill -f "$service" || true
    done
    
    # Stop Docker Compose test services
    cd "$PROJECT_ROOT/shared/docker-compose"
    docker-compose -f docker-compose.test.yml --profile test down || true
    
    print_status "Cleanup completed"
}

# Set up trap for cleanup
trap cleanup EXIT

# Function to wait for service to be ready
wait_for_service() {
    local service_name=$1
    local port=$2
    local max_attempts=30
    local attempt=1
    
    print_status "Waiting for $service_name to be ready on port $port..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
            print_status "$service_name is ready!"
            return 0
        fi
        
        echo -n "."
        sleep 2
        ((attempt++))
    done
    
    print_error "$service_name failed to start within expected time"
    return 1
}

# Function to run tests for a service
run_service_tests() {
    local service=$1
    local service_dir="$PROJECT_ROOT/$service"
    
    print_status "Running tests for $service..."
    
    cd "$service_dir"
    
    # Run unit tests
    print_status "Running unit tests for $service..."
    ./mvnw test -Dspring.profiles.active=$TEST_PROFILE
    
    # Run integration tests
    print_status "Running integration tests for $service..."
    ./mvnw verify -Dspring.profiles.active=$TEST_PROFILE -Dtest="*IntegrationTest"
    
    print_status "Tests completed for $service ✅"
}

# Function to create test data
setup_test_data() {
    print_status "Setting up test data..."
    
    # Create SQLite directories
    mkdir -p "$PROJECT_ROOT/shared/sqlite-data/test"
    
    # Initialize test databases with schema
    for service in "${SERVICES[@]}"; do
        schema_file="$PROJECT_ROOT/$service/schema/${service%-service}-schema.json"
        if [ -f "$schema_file" ]; then
            print_status "Loading schema for $service..."
            # Schema loading would be done here in a real implementation
        fi
    done
    
    print_status "Test data setup completed"
}

# Function to run distributed transaction tests
run_distributed_tests() {
    print_status "Running distributed transaction tests..."
    
    cd "$PROJECT_ROOT/order-service"
    
    # Run comprehensive integration tests
    ./mvnw test -Dspring.profiles.active=$TEST_PROFILE -Dtest="OrderProcessIntegrationTest"
    
    print_status "Distributed transaction tests completed ✅"
}

# Function to generate test report
generate_test_report() {
    print_status "Generating test report..."
    
    local report_dir="$PROJECT_ROOT/test-reports"
    mkdir -p "$report_dir"
    
    # Aggregate test results
    echo "# Integration Test Report" > "$report_dir/integration-test-report.md"
    echo "Generated on: $(date)" >> "$report_dir/integration-test-report.md"
    echo "" >> "$report_dir/integration-test-report.md"
    
    for service in "${SERVICES[@]}"; do
        echo "## $service" >> "$report_dir/integration-test-report.md"
        
        # Check if test results exist
        test_results="$PROJECT_ROOT/$service/target/surefire-reports"
        if [ -d "$test_results" ]; then
            test_count=$(find "$test_results" -name "TEST-*.xml" | wc -l)
            echo "- Test files: $test_count" >> "$report_dir/integration-test-report.md"
        else
            echo "- No test results found" >> "$report_dir/integration-test-report.md"
        fi
        echo "" >> "$report_dir/integration-test-report.md"
    done
    
    print_status "Test report generated at $report_dir/integration-test-report.md"
}

# Main execution
main() {
    print_status "Starting microservices integration test suite..."
    
    # Change to project root
    cd "$PROJECT_ROOT"
    
    # Setup test environment
    setup_test_data
    
    # Start test infrastructure
    print_status "Starting test infrastructure..."
    cd "$PROJECT_ROOT/shared/docker-compose"
    docker-compose -f docker-compose.test.yml --profile test up -d
    
    # Wait for infrastructure to be ready
    sleep 10
    
    # Run tests for each service
    local failed_services=()
    
    for service in "${SERVICES[@]}"; do
        if ! run_service_tests "$service"; then
            failed_services+=("$service")
            print_error "Tests failed for $service"
        fi
    done
    
    # Run distributed transaction tests
    if ! run_distributed_tests; then
        print_error "Distributed transaction tests failed"
        failed_services+=("distributed-tests")
    fi
    
    # Generate test report
    generate_test_report
    
    # Summary
    if [ ${#failed_services[@]} -eq 0 ]; then
        print_status "🎉 All integration tests passed successfully!"
        exit 0
    else
        print_error "❌ The following tests failed: ${failed_services[*]}"
        exit 1
    fi
}

# Check if script is being run directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi