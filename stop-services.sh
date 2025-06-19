#!/bin/bash

# Microservices Shutdown Script
set -e

echo "🛑 Stopping Microservices System..."

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

# Function to check if port is in use
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Function to stop a service by PID file
stop_service_by_pid() {
    local service_name=$1
    local pid_file="$PID_DIR/$service_name.pid"
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if kill -0 "$pid" 2>/dev/null; then
            print_status "Stopping $service_name (PID: $pid)..."
            
            # Try graceful shutdown first
            kill -TERM "$pid" 2>/dev/null || true
            
            # Wait up to 30 seconds for graceful shutdown
            local count=0
            while [ $count -lt 30 ] && kill -0 "$pid" 2>/dev/null; do
                sleep 1
                ((count++))
            done
            
            # Force kill if still running
            if kill -0 "$pid" 2>/dev/null; then
                print_warning "Force killing $service_name (PID: $pid)..."
                kill -KILL "$pid" 2>/dev/null || true
                sleep 2
            fi
            
            # Check if process is really dead
            if ! kill -0 "$pid" 2>/dev/null; then
                print_status "$service_name stopped successfully"
                rm -f "$pid_file"
                return 0
            else
                print_error "Failed to stop $service_name (PID: $pid)"
                return 1
            fi
        else
            print_warning "$service_name PID file exists but process is not running"
            rm -f "$pid_file"
            return 0
        fi
    else
        print_warning "No PID file found for $service_name"
        return 0
    fi
}

# Function to stop service by port
stop_service_by_port() {
    local service_name=$1
    local port=$2
    
    if check_port $port; then
        print_status "Stopping $service_name on port $port..."
        
        # Get process IDs listening on the port
        local pids=$(lsof -ti:$port)
        
        if [ -n "$pids" ]; then
            for pid in $pids; do
                if kill -0 "$pid" 2>/dev/null; then
                    print_status "Stopping process $pid on port $port..."
                    
                    # Try graceful shutdown first
                    kill -TERM "$pid" 2>/dev/null || true
                    
                    # Wait up to 15 seconds for graceful shutdown
                    local count=0
                    while [ $count -lt 15 ] && kill -0 "$pid" 2>/dev/null; do
                        sleep 1
                        ((count++))
                    done
                    
                    # Force kill if still running
                    if kill -0 "$pid" 2>/dev/null; then
                        print_warning "Force killing process $pid..."
                        kill -KILL "$pid" 2>/dev/null || true
                    fi
                fi
            done
            
            # Wait a moment and check if port is free
            sleep 2
            if ! check_port $port; then
                print_status "$service_name on port $port stopped successfully"
                return 0
            else
                print_error "Port $port is still in use after stopping $service_name"
                return 1
            fi
        else
            print_warning "No processes found listening on port $port"
            return 0
        fi
    else
        print_status "$service_name is not running on port $port"
        return 0
    fi
}

# Function to stop a Spring Boot service
stop_service() {
    local service_name=$1
    local port=$(get_service_port "$service_name")
    
    print_header "Stopping $service_name..."
    
    # Try to stop by PID file first
    if stop_service_by_pid "$service_name"; then
        return 0
    fi
    
    # If PID file method failed, try by port
    if stop_service_by_port "$service_name" "$port"; then
        return 0
    fi
    
    return 1
}

# Function to stop frontend
stop_frontend() {
    print_header "Stopping Frontend..."
    
    # Try to stop by PID file first
    if stop_service_by_pid "frontend"; then
        return 0
    fi
    
    # If PID file method failed, try by port
    if stop_service_by_port "frontend" "$FRONTEND_PORT"; then
        return 0
    fi
    
    return 1
}

# Function to stop monitoring stack
stop_monitoring() {
    print_header "Stopping Monitoring Stack..."
    
    local monitoring_script="$PROJECT_ROOT/shared/scripts/start-monitoring.sh"
    
    if [ -f "$monitoring_script" ]; then
        print_status "Stopping monitoring services..."
        bash "$monitoring_script" stop 2>/dev/null || true
    else
        print_warning "Monitoring script not found, trying to stop common monitoring ports..."
        
        # Try to stop common monitoring ports
        local monitoring_ports=(9090 3001 8080)
        for port in "${monitoring_ports[@]}"; do
            if check_port $port && [ $port -ne $FRONTEND_PORT ]; then
                print_status "Stopping service on monitoring port $port..."
                stop_service_by_port "monitoring-$port" "$port" || true
            fi
        done
    fi
}

# Function to clean up stale PID files
cleanup_pid_files() {
    print_header "Cleaning up PID files..."
    
    if [ -d "$PID_DIR" ]; then
        for pid_file in "$PID_DIR"/*.pid; do
            if [ -f "$pid_file" ]; then
                local pid=$(cat "$pid_file" 2>/dev/null || echo "")
                local service_name=$(basename "$pid_file" .pid)
                
                if [ -n "$pid" ]; then
                    if ! kill -0 "$pid" 2>/dev/null; then
                        print_status "Removing stale PID file for $service_name"
                        rm -f "$pid_file"
                    fi
                else
                    print_status "Removing empty PID file for $service_name"
                    rm -f "$pid_file"
                fi
            fi
        done
    fi
}

# Function to kill all Java processes (emergency cleanup)
emergency_cleanup() {
    print_header "Emergency cleanup - stopping all Java processes..."
    
    # Find Java processes that might be our services
    local java_pids=$(pgrep -f "spring-boot:run" 2>/dev/null || true)
    
    if [ -n "$java_pids" ]; then
        print_warning "Found Spring Boot processes: $java_pids"
        
        for pid in $java_pids; do
            if kill -0 "$pid" 2>/dev/null; then
                local cmd=$(ps -p "$pid" -o command= 2>/dev/null || echo "unknown")
                print_warning "Stopping Java process $pid: $cmd"
                kill -TERM "$pid" 2>/dev/null || true
            fi
        done
        
        # Wait for graceful shutdown
        sleep 5
        
        # Force kill any remaining processes
        java_pids=$(pgrep -f "spring-boot:run" 2>/dev/null || true)
        if [ -n "$java_pids" ]; then
            print_warning "Force killing remaining Java processes..."
            for pid in $java_pids; do
                kill -KILL "$pid" 2>/dev/null || true
            done
        fi
    fi
    
    # Also check for Node.js processes (frontend)
    local node_pids=$(pgrep -f "npm run dev\|next dev" 2>/dev/null || true)
    
    if [ -n "$node_pids" ]; then
        print_warning "Found Node.js processes: $node_pids"
        
        for pid in $node_pids; do
            if kill -0 "$pid" 2>/dev/null; then
                print_warning "Stopping Node.js process $pid"
                kill -TERM "$pid" 2>/dev/null || true
            fi
        done
        
        sleep 3
        
        # Force kill any remaining Node.js processes
        node_pids=$(pgrep -f "npm run dev\|next dev" 2>/dev/null || true)
        if [ -n "$node_pids" ]; then
            for pid in $node_pids; do
                kill -KILL "$pid" 2>/dev/null || true
            done
        fi
    fi
}

# Function to show final status
show_final_status() {
    print_header "Final Service Status"
    echo ""
    
    local all_stopped=true
    
    # Check backend services
    echo "📊 Backend Services:"
    local names=($SERVICE_NAMES)
    local ports=($SERVICE_PORTS)
    for i in "${!names[@]}"; do
        local service_name="${names[$i]}"
        local port="${ports[$i]}"
        if check_port $port; then
            echo -e "  ❌ $service_name - Still running on port $port"
            all_stopped=false
        else
            echo -e "  ✅ $service_name - Stopped"
        fi
    done
    
    echo ""
    echo "🌐 Frontend:"
    if check_port $FRONTEND_PORT; then
        echo -e "  ❌ Frontend - Still running on port $FRONTEND_PORT"
        all_stopped=false
    else
        echo -e "  ✅ Frontend - Stopped"
    fi
    
    echo ""
    echo "📈 Monitoring:"
    local monitoring_ports=(9090 3001)
    for port in "${monitoring_ports[@]}"; do
        if check_port $port && [ $port -ne $FRONTEND_PORT ]; then
            echo -e "  ❌ Service on port $port - Still running"
            all_stopped=false
        fi
    done
    
    if $all_stopped; then
        echo -e "  ✅ All monitoring services - Stopped"
    fi
    
    echo ""
    
    if $all_stopped; then
        print_status "🎉 All services stopped successfully!"
        echo ""
        echo "📁 Logs preserved in: $LOG_DIR"
        echo "🧹 PID files cleaned up: $PID_DIR"
    else
        print_warning "⚠️  Some services may still be running"
        echo ""
        echo "🔍 Use 'ps aux | grep -E \"spring-boot|npm run dev\"' to check manually"
        echo "💀 Use '$0 emergency' for force cleanup"
    fi
}

# Main shutdown sequence
main() {
    local shutdown_mode="${1:-all}"
    
    print_header "🛑 Microservices System Shutdown"
    echo "Mode: $shutdown_mode"
    echo "Project Root: $PROJECT_ROOT"
    echo ""
    
    case $shutdown_mode in
        "all")
            # Stop services in reverse order (Order service first, then dependencies)
            print_header "Stopping Backend Services..."
            local failed_services=()
            
            for service_name in order-service shipping-service payment-service inventory-service; do
                if ! stop_service "$service_name"; then
                    failed_services+=("$service_name")
                fi
            done
            
            # Stop frontend
            stop_frontend
            
            # Stop monitoring
            stop_monitoring
            
            # Clean up PID files
            cleanup_pid_files
            
            # Show final status
            echo ""
            show_final_status
            
            if [ ${#failed_services[@]} -gt 0 ]; then
                print_error "❌ Some services failed to stop cleanly: ${failed_services[*]}"
                echo ""
                echo "💀 Use '$0 emergency' for force cleanup"
                exit 1
            fi
            ;;
        "backend")
            for service_name in order-service shipping-service payment-service inventory-service; do
                stop_service "$service_name"
            done
            cleanup_pid_files
            ;;
        "frontend")
            stop_frontend
            ;;
        "monitoring")
            stop_monitoring
            ;;
        "emergency")
            print_warning "🚨 Emergency cleanup mode - force stopping all services"
            emergency_cleanup
            cleanup_pid_files
            show_final_status
            ;;
        "status")
            show_final_status
            ;;
        "clean")
            print_header "Cleaning up logs and PID files..."
            
            # Remove PID files
            if [ -d "$PID_DIR" ]; then
                rm -rf "$PID_DIR"
                print_status "PID directory cleaned: $PID_DIR"
            fi
            
            # Optionally clean logs (ask for confirmation)
            if [ -d "$LOG_DIR" ] && [ "$(ls -A "$LOG_DIR" 2>/dev/null)" ]; then
                echo -n "Remove all log files in $LOG_DIR? [y/N]: "
                read -r response
                if [[ "$response" =~ ^[Yy]$ ]]; then
                    rm -rf "$LOG_DIR"
                    print_status "Log directory cleaned: $LOG_DIR"
                else
                    print_status "Log files preserved in: $LOG_DIR"
                fi
            else
                print_status "No log files to clean"
            fi
            ;;
        *)
            echo "Usage: $0 [all|backend|frontend|monitoring|emergency|status|clean]"
            echo ""
            echo "Options:"
            echo "  all        Stop all services (default)"
            echo "  backend    Stop only backend services"
            echo "  frontend   Stop only frontend"
            echo "  monitoring Stop only monitoring stack"
            echo "  emergency  Force stop all Java and Node.js processes"
            echo "  status     Show current service status"
            echo "  clean      Clean up logs and PID files"
            echo ""
            echo "Examples:"
            echo "  $0              # Stop all services gracefully"
            echo "  $0 backend      # Stop only Spring Boot services"
            echo "  $0 emergency    # Force stop everything"
            echo "  $0 clean        # Clean up after shutdown"
            exit 1
            ;;
    esac
}

# Cleanup function for script interruption
cleanup_script() {
    print_status "Script interrupted, cleaning up..."
    # Any cleanup needed if script is interrupted
}

trap cleanup_script INT TERM

# Check if script is being run directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi