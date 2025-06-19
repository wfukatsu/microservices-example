#!/bin/bash

# Monitoring Stack Startup Script
set -e

echo "🔍 Starting Monitoring Stack for Microservices..."

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

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}[MONITORING]${NC} $1"
}

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DOCKER_COMPOSE_DIR="$PROJECT_ROOT/shared/docker-compose"
MONITORING_DIR="$PROJECT_ROOT/shared/monitoring"

# Function to check Docker installation
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    print_status "Docker and Docker Compose are available ✅"
}

# Function to start monitoring stack
start_monitoring() {
    print_header "Starting monitoring infrastructure..."
    
    cd "$DOCKER_COMPOSE_DIR"
    
    # Pull latest images
    print_status "Pulling latest monitoring images..."
    docker-compose -f docker-compose.monitoring.yml pull
    
    # Start monitoring stack
    print_status "Starting monitoring services..."
    docker-compose -f docker-compose.monitoring.yml up -d
    
    print_status "Monitoring stack started successfully! 🎉"
}

# Function to wait for services to be ready
wait_for_services() {
    local services=("prometheus:9090" "grafana:3000" "alertmanager:9093" "jaeger:16686")
    
    print_header "Waiting for monitoring services to be ready..."
    
    for service in "${services[@]}"; do
        local name=$(echo $service | cut -d':' -f1)
        local port=$(echo $service | cut -d':' -f2)
        
        print_status "Waiting for $name to be ready..."
        
        local max_attempts=30
        local attempt=1
        
        while [ $attempt -le $max_attempts ]; do
            if curl -f "http://localhost:$port" >/dev/null 2>&1; then
                print_status "$name is ready! ✅"
                break
            fi
            
            if [ $attempt -eq $max_attempts ]; then
                print_warning "$name is not ready after 60 seconds"
                break
            fi
            
            echo -n "."
            sleep 2
            ((attempt++))
        done
    done
}

# Function to setup Grafana dashboards
setup_grafana() {
    print_header "Setting up Grafana dashboards..."
    
    # Wait for Grafana to be fully ready
    sleep 10
    
    # Import dashboard
    local dashboard_file="$MONITORING_DIR/grafana-dashboard.json"
    if [ -f "$dashboard_file" ]; then
        print_status "Importing microservices dashboard..."
        
        curl -X POST \
            -H "Content-Type: application/json" \
            -d @"$dashboard_file" \
            "http://admin:admin123@localhost:3000/api/dashboards/db" \
            >/dev/null 2>&1 || print_warning "Failed to import dashboard automatically"
    fi
    
    print_status "Grafana setup completed!"
}

# Function to display access information
show_access_info() {
    print_header "Monitoring Services Access Information"
    echo ""
    echo "📊 Grafana Dashboard:"
    echo "   URL: http://localhost:3000"
    echo "   Username: admin"
    echo "   Password: admin123"
    echo ""
    echo "📈 Prometheus:"
    echo "   URL: http://localhost:9090"
    echo ""
    echo "🚨 AlertManager:"
    echo "   URL: http://localhost:9093"
    echo ""
    echo "🔍 Jaeger Tracing:"
    echo "   URL: http://localhost:16686"
    echo ""
    echo "📝 Loki Logs:"
    echo "   URL: http://localhost:3100"
    echo ""
    echo "💻 Node Exporter:"
    echo "   URL: http://localhost:9100"
    echo ""
    echo "🐳 cAdvisor:"
    echo "   URL: http://localhost:8080"
    echo ""
    print_status "All monitoring services are now available! 🎉"
}

# Function to check monitoring health
check_monitoring_health() {
    print_header "Checking monitoring stack health..."
    
    local services=("prometheus:9090" "grafana:3000" "alertmanager:9093" "jaeger:16686" "loki:3100")
    local healthy=true
    
    for service in "${services[@]}"; do
        local name=$(echo $service | cut -d':' -f1)
        local port=$(echo $service | cut -d':' -f2)
        
        if curl -f "http://localhost:$port" >/dev/null 2>&1; then
            print_status "$name is healthy ✅"
        else
            print_error "$name is not responding ❌"
            healthy=false
        fi
    done
    
    if [ "$healthy" = true ]; then
        print_status "All monitoring services are healthy! 🎉"
        return 0
    else
        print_error "Some monitoring services are not healthy"
        return 1
    fi
}

# Function to show logs
show_logs() {
    print_header "Showing monitoring services logs..."
    cd "$DOCKER_COMPOSE_DIR"
    docker-compose -f docker-compose.monitoring.yml logs -f
}

# Function to stop monitoring
stop_monitoring() {
    print_header "Stopping monitoring stack..."
    cd "$DOCKER_COMPOSE_DIR"
    docker-compose -f docker-compose.monitoring.yml down
    print_status "Monitoring stack stopped."
}

# Function to restart monitoring
restart_monitoring() {
    print_header "Restarting monitoring stack..."
    stop_monitoring
    sleep 5
    start_monitoring
    wait_for_services
    print_status "Monitoring stack restarted successfully!"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  start     Start the monitoring stack"
    echo "  stop      Stop the monitoring stack"
    echo "  restart   Restart the monitoring stack"
    echo "  status    Check monitoring services health"
    echo "  logs      Show monitoring services logs"
    echo "  info      Show access information"
    echo "  help      Show this help message"
    echo ""
    echo "If no command is provided, 'start' is assumed."
}

# Main execution
main() {
    local command="${1:-start}"
    
    case $command in
        start)
            check_docker
            start_monitoring
            wait_for_services
            setup_grafana
            show_access_info
            ;;
        stop)
            stop_monitoring
            ;;
        restart)
            check_docker
            restart_monitoring
            show_access_info
            ;;
        status)
            check_monitoring_health
            ;;
        logs)
            show_logs
            ;;
        info)
            show_access_info
            ;;
        help|--help|-h)
            show_usage
            ;;
        *)
            print_error "Unknown command: $command"
            show_usage
            exit 1
            ;;
    esac
}

# Check if script is being run directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi