package com.example.inventory.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class MetricsConfig {

    @Bean
    public InventoryMetrics inventoryMetrics(MeterRegistry meterRegistry) {
        return new InventoryMetrics(meterRegistry);
    }

    @Component
    public static class InventoryMetrics {
        
        private final Counter reservationSuccessCounter;
        private final Counter reservationFailureCounter;
        private final Counter confirmationCounter;
        private final Counter cancellationCounter;
        private final Timer reservationProcessingTimer;
        private final AtomicInteger activeReservations = new AtomicInteger(0);
        private final AtomicInteger lowStockProducts = new AtomicInteger(0);

        public InventoryMetrics(MeterRegistry meterRegistry) {
            // Counters
            this.reservationSuccessCounter = Counter.builder("inventory_reservation_total")
                .description("Total number of successful inventory reservations")
                .tag("status", "success")
                .register(meterRegistry);
            
            this.reservationFailureCounter = Counter.builder("inventory_reservation_total")
                .description("Total number of failed inventory reservations")
                .tag("status", "failed")
                .register(meterRegistry);
            
            this.confirmationCounter = Counter.builder("inventory_confirmation_total")
                .description("Total number of inventory confirmations")
                .register(meterRegistry);
            
            this.cancellationCounter = Counter.builder("inventory_cancellation_total")
                .description("Total number of inventory cancellations")
                .register(meterRegistry);
            
            // Timer
            this.reservationProcessingTimer = Timer.builder("inventory_reservation_duration_seconds")
                .description("Time taken to process inventory reservations")
                .register(meterRegistry);
            
            // Gauges
            Gauge.builder("inventory_active_reservations")
                .description("Number of active inventory reservations")
                .register(meterRegistry, this, metrics -> metrics.activeReservations.get());
            
            Gauge.builder("inventory_low_stock_products")
                .description("Number of products with low stock")
                .register(meterRegistry, this, metrics -> metrics.lowStockProducts.get());
        }

        public void incrementReservationSuccess() {
            reservationSuccessCounter.increment();
            activeReservations.incrementAndGet();
        }

        public void incrementReservationFailure() {
            reservationFailureCounter.increment();
        }

        public void incrementConfirmation() {
            confirmationCounter.increment();
            activeReservations.decrementAndGet();
        }

        public void incrementCancellation() {
            cancellationCounter.increment();
            activeReservations.decrementAndGet();
        }

        public Timer.Sample startReservationTimer() {
            return Timer.start();
        }

        public void recordReservationTime(Timer.Sample sample) {
            sample.stop(reservationProcessingTimer);
        }

        public void setLowStockProducts(int count) {
            lowStockProducts.set(count);
        }

        public void decrementActiveReservations() {
            activeReservations.decrementAndGet();
        }
    }
}