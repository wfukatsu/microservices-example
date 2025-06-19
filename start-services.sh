#!/bin/bash

# Microservices Startup Script
set -e

echo "🚀 Starting Microservices System..."

# Colors for output (check if terminal supports colors)
if [ -t 1 ] && command -v tput >/dev/null 2>&1 && [ "$(tput colors 2>/dev/null || echo 0)" -ge 8 ]; then
    RED=$(tput setaf 1)
    GREEN=$(tput setaf 2)
    YELLOW=$(tput setaf 3)
    BLUE=$(tput setaf 4)
    NC=$(tput sgr0)
else
    RED=''
    GREEN=''
    YELLOW=''
    BLUE=''
    NC=''
fi

# Function to print colored output
print_status() {
    printf "${GREEN}[INFO]${NC} %s\n" "$1"
}

print_warning() {
    printf "${YELLOW}[WARN]${NC} %s\n" "$1"
}

print_error() {
    printf "${RED}[ERROR]${NC} %s\n" "$1"
}

print_header() {
    printf "${BLUE}[SYSTEM]${NC} %s\n" "$1"
}

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$PROJECT_ROOT/logs"
PID_DIR="$PROJECT_ROOT/.pids"

# Create necessary directories
mkdir -p "$LOG_DIR"
mkdir -p "$PID_DIR"
mkdir -p "$PROJECT_ROOT/shared/sqlite-data"

# Services configuration
SERVICE_NAMES="inventory-service payment-service shipping-service order-service"
SERVICE_PORTS="8081 8082 8083 8080"

# Function to get port for service
get_service_port() {
    local service_name=$1
    local names=($SERVICE_NAMES)
    local ports=($SERVICE_PORTS)
    
    for i in "${!names[@]}"; do
        if [[ "${names[$i]}" == "$service_name" ]]; then
            echo "${ports[$i]}"
            return 0
        fi
    done
    return 1
}

FRONTEND_PORT=3000
MONITORING_ENABLED=${MONITORING_ENABLED:-true}

# Function to check if port is in use
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Function to wait for service to be ready
wait_for_service() {
    local service_name=$1
    local port=$2
    local max_attempts=60
    local attempt=1
    
    print_status "Waiting for $service_name to be ready on port $port..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
            print_status "$service_name is ready! ✅"
            return 0
        fi
        
        if [ $((attempt % 10)) -eq 0 ]; then
            echo -n " (${attempt}s)"
        else
            echo -n "."
        fi
        
        sleep 1
        ((attempt++))
    done
    
    print_error "$service_name failed to start within 60 seconds"
    return 1
}

# Function to start a Spring Boot service
start_service() {
    local service_name=$1
    local port=$(get_service_port "$service_name")
    local service_dir="$PROJECT_ROOT/$service_name"
    local log_file="$LOG_DIR/$service_name.log"
    local pid_file="$PID_DIR/$service_name.pid"
    
    print_header "Starting $service_name on port $port..."
    
    # Check if service is already running
    if check_port $port; then
        print_warning "$service_name is already running on port $port"
        return 0
    fi
    
    # Check if service directory exists
    if [ ! -d "$service_dir" ]; then
        print_error "Service directory not found: $service_dir"
        return 1
    fi
    
    # Navigate to service directory and start
    cd "$service_dir"
    
    # Check if Maven wrapper exists
    if [ ! -f "./mvnw" ]; then
        print_error "Maven wrapper not found in $service_dir"
        return 1
    fi
    
    # Make Maven wrapper executable
    chmod +x ./mvnw
    
    # Start the service in background
    print_status "Starting $service_name..."
    nohup ./mvnw spring-boot:run > "$log_file" 2>&1 &
    local service_pid=$!
    
    # Save PID
    echo $service_pid > "$pid_file"
    
    print_status "$service_name started with PID $service_pid"
    
    # Wait for service to be ready
    if wait_for_service "$service_name" "$port"; then
        return 0
    else
        # Kill the process if it failed to start properly
        kill $service_pid 2>/dev/null || true
        rm -f "$pid_file"
        return 1
    fi
}

# Function to start frontend
start_frontend() {
    local frontend_dir="$PROJECT_ROOT/frontend"
    local log_file="$LOG_DIR/frontend.log"
    local pid_file="$PID_DIR/frontend.pid"
    
    print_header "Starting Frontend on port $FRONTEND_PORT..."
    
    # Check if frontend is already running
    if check_port $FRONTEND_PORT; then
        print_warning "Frontend is already running on port $FRONTEND_PORT"
        return 0
    fi
    
    # Check if frontend directory exists
    if [ ! -d "$frontend_dir" ]; then
        print_warning "Frontend directory not found: $frontend_dir"
        print_warning "Skipping frontend startup"
        return 0
    fi
    
    cd "$frontend_dir"
    
    # Check if node_modules exists
    if [ ! -d "node_modules" ]; then
        print_status "Installing frontend dependencies..."
        npm install
    fi
    
    # Start frontend in background
    print_status "Starting frontend..."
    nohup npm run dev > "$log_file" 2>&1 &
    local frontend_pid=$!
    
    # Save PID
    echo $frontend_pid > "$pid_file"
    
    print_status "Frontend started with PID $frontend_pid"
    
    # Wait for frontend to be ready
    local max_attempts=30
    local attempt=1
    
    print_status "Waiting for frontend to be ready..."
    while [ $attempt -le $max_attempts ]; do
        if curl -f "http://localhost:$FRONTEND_PORT" >/dev/null 2>&1; then
            print_status "Frontend is ready! ✅"
            return 0
        fi
        
        echo -n "."
        sleep 2
        ((attempt++))
    done
    
    print_warning "Frontend may still be starting up..."
    return 0
}

# Function to start monitoring stack
start_monitoring() {
    if [ "$MONITORING_ENABLED" != "true" ]; then
        print_status "Monitoring disabled, skipping..."
        return 0
    fi
    
    print_header "Starting Monitoring Stack..."
    
    local monitoring_script="$PROJECT_ROOT/shared/scripts/start-monitoring.sh"
    
    if [ -f "$monitoring_script" ]; then
        print_status "Starting monitoring services..."
        bash "$monitoring_script" start
    else
        print_warning "Monitoring script not found: $monitoring_script"
        print_warning "Skipping monitoring startup"
    fi
}

# Function to initialize database schemas
init_schemas() {
    print_header "Initializing Database Schemas..."
    
    local names=($SERVICE_NAMES)
    for service_name in "${names[@]}"; do
        local schema_file="$PROJECT_ROOT/$service_name/schema/${service_name%-service}-schema.json"
        
        if [ -f "$schema_file" ]; then
            print_status "Schema found for $service_name: $schema_file"
            # In a real implementation, you would load the schema here
            # java -jar scalardb-schema-loader.jar --config scalardb.properties --schema-file "$schema_file"
        else
            print_warning "Schema file not found for $service_name"
        fi
    done
}

# Function to show service status
show_status() {
    print_header "Service Status Summary"
    echo ""
    
    # Check backend services
    echo "📊 Backend Services:"
    local names=($SERVICE_NAMES)
    local ports=($SERVICE_PORTS)
    for i in "${!names[@]}"; do
        local service_name="${names[$i]}"
        local port="${ports[$i]}"
        if check_port $port; then
            echo -e "  ✅ $service_name - http://localhost:$port"
        else
            echo -e "  ❌ $service_name - Not running"
        fi
    done
    
    echo ""
    echo "🌐 Frontend:"
    if check_port $FRONTEND_PORT; then
        echo -e "  ✅ Frontend - http://localhost:$FRONTEND_PORT"
    else
        echo -e "  ❌ Frontend - Not running"
    fi
    
    echo ""
    echo "📈 Monitoring:"
    if check_port 9090; then
        echo -e "  ✅ Prometheus - http://localhost:9090"
    else
        echo -e "  ❌ Prometheus - Not running"
    fi
    
    if check_port 3000; then
        local grafana_port=3000
        if check_port $FRONTEND_PORT && [ $FRONTEND_PORT -eq 3000 ]; then
            grafana_port=3001
        fi
        if check_port $grafana_port; then
            echo -e "  ✅ Grafana - http://localhost:$grafana_port"
        else
            echo -e "  ❌ Grafana - Not running"
        fi
    fi
    
    echo ""
    echo "📝 Logs: $LOG_DIR"
    echo "🔍 PIDs: $PID_DIR"
}

# Function to check dependencies
check_dependencies() {
    print_header "Checking Dependencies..."
    
    # Check Java
    if command -v java >/dev/null 2>&1; then
        local java_version=$(java -version 2>&1 | head -n 1)
        print_status "Java: $java_version"
    else
        print_error "Java is not installed"
        return 1
    fi
    
    # Check Maven
    if command -v mvn >/dev/null 2>&1; then
        local maven_version=$(mvn -version 2>&1 | head -n 1)
        print_status "Maven: $maven_version"
    else
        print_warning "Maven not found, will use Maven wrapper"
    fi
    
    # Check Node.js
    if command -v node >/dev/null 2>&1; then
        local node_version=$(node --version)
        print_status "Node.js: $node_version"
    else
        print_warning "Node.js not found, frontend will not start"
    fi
    
    # Check Docker (for monitoring)
    if command -v docker >/dev/null 2>&1; then
        print_status "Docker: Available"
    else
        print_warning "Docker not found, monitoring stack will not start"
        MONITORING_ENABLED=false
    fi
    
    return 0
}

# Main startup sequence
main() {
    local start_mode="${1:-all}"
    
    print_header "🚀 Microservices System Startup"
    echo "Mode: $start_mode"
    echo "Project Root: $PROJECT_ROOT"
    echo ""
    
    # Check dependencies
    if ! check_dependencies; then
        print_error "Dependency check failed"
        exit 1
    fi
    
    case $start_mode in
        "all")
            # Initialize schemas
            init_schemas
            
            # Start backend services in order
            print_header "Starting Backend Services..."
            local failed_services=()
            
            for service_name in inventory-service payment-service shipping-service order-service; do
                if ! start_service "$service_name"; then
                    failed_services+=("$service_name")
                    print_error "$service_name failed to start"
                fi
            done
            
            # Start frontend
            start_frontend
            
            # Start monitoring
            start_monitoring
            
            # Show final status
            echo ""
            show_status
            
            if [ ${#failed_services[@]} -eq 0 ]; then
                echo ""
                print_status "🎉 All services started successfully!"
                echo ""
                echo "📱 Access the application:"
                echo "   Frontend: http://localhost:$FRONTEND_PORT"
                echo "   Order API: http://localhost:8080/actuator/health"
                echo "   Inventory API: http://localhost:8081/actuator/health"
                echo "   Payment API: http://localhost:8082/actuator/health"
                echo "   Shipping API: http://localhost:8083/actuator/health"
                echo ""
                echo "📊 Monitoring:"
                echo "   Prometheus: http://localhost:9090"
                echo "   Grafana: http://localhost:3001 (admin/admin123)"
                echo ""
                echo "📝 View logs: tail -f $LOG_DIR/*.log"
                echo "🛑 Stop services: ./stop-services.sh"
            else
                print_error "❌ Some services failed to start: ${failed_services[*]}"
                echo ""
                echo "📝 Check logs in: $LOG_DIR"
                echo "🛑 Stop services: ./stop-services.sh"
                exit 1
            fi
            ;;
        "backend")
            init_schemas
            local names=($SERVICE_NAMES)
            for service_name in "${names[@]}"; do
                start_service "$service_name"
            done
            ;;
        "frontend")
            start_frontend
            ;;
        "monitoring")
            start_monitoring
            ;;
        "status")
            show_status
            ;;
        *)
            echo "Usage: $0 [all|backend|frontend|monitoring|status]"
            echo ""
            echo "Options:"
            echo "  all        Start all services (default)"
            echo "  backend    Start only backend services"
            echo "  frontend   Start only frontend"
            echo "  monitoring Start only monitoring stack"
            echo "  status     Show service status"
            echo ""
            echo "Environment Variables:"
            echo "  MONITORING_ENABLED=true/false (default: true)"
            exit 1
            ;;
    esac
}

# Cleanup function
cleanup() {
    print_status "Cleaning up..."
    # Kill any background processes started by this script
    jobs -p | xargs -r kill 2>/dev/null || true
}

trap cleanup EXIT

# Check if script is being run directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi