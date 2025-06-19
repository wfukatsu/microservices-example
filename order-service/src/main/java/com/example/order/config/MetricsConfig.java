package com.example.order.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class MetricsConfig {

    @Bean
    public OrderMetrics orderMetrics(MeterRegistry meterRegistry) {
        return new OrderMetrics(meterRegistry);
    }

    @Component
    public static class OrderMetrics {
        
        private final Counter orderSuccessCounter;
        private final Counter orderFailureCounter;
        private final Counter orderCancellationCounter;
        private final Timer orderProcessingTimer;
        private final Timer distributedTransactionTimer;
        private final AtomicInteger activeOrders = new AtomicInteger(0);
        private final AtomicInteger failedCompensations = new AtomicInteger(0);

        public OrderMetrics(MeterRegistry meterRegistry) {
            // Counters
            this.orderSuccessCounter = Counter.builder("order_processing_total")
                .description("Total number of successful order processing")
                .tag("status", "success")
                .register(meterRegistry);
            
            this.orderFailureCounter = Counter.builder("order_processing_total")
                .description("Total number of failed order processing")
                .tag("status", "failed")
                .register(meterRegistry);
            
            this.orderCancellationCounter = Counter.builder("order_cancellation_total")
                .description("Total number of order cancellations")
                .register(meterRegistry);
            
            // Timers
            this.orderProcessingTimer = Timer.builder("order_processing_duration_seconds")
                .description("Time taken to process orders")
                .register(meterRegistry);
            
            this.distributedTransactionTimer = Timer.builder("distributed_transaction_duration_seconds")
                .description("Time taken for distributed transactions")
                .register(meterRegistry);
            
            // Gauges
            Gauge.builder("order_active_processing")
                .description("Number of orders currently being processed")
                .register(meterRegistry, this, metrics -> metrics.activeOrders.get());
            
            Gauge.builder("order_failed_compensations")
                .description("Number of failed compensation transactions")
                .register(meterRegistry, this, metrics -> metrics.failedCompensations.get());
        }

        public void incrementOrderSuccess() {
            orderSuccessCounter.increment();
            activeOrders.decrementAndGet();
        }

        public void incrementOrderFailure() {
            orderFailureCounter.increment();
            activeOrders.decrementAndGet();
        }

        public void incrementOrderCancellation() {
            orderCancellationCounter.increment();
        }

        public Timer.Sample startOrderProcessingTimer() {
            activeOrders.incrementAndGet();
            return Timer.start();
        }

        public void recordOrderProcessingTime(Timer.Sample sample) {
            sample.stop(orderProcessingTimer);
        }

        public Timer.Sample startDistributedTransactionTimer() {
            return Timer.start();
        }

        public void recordDistributedTransactionTime(Timer.Sample sample) {
            sample.stop(distributedTransactionTimer);
        }

        public void incrementFailedCompensation() {
            failedCompensations.incrementAndGet();
        }

        public void decrementActiveOrders() {
            activeOrders.decrementAndGet();
        }
    }
}