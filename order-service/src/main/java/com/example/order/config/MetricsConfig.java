package com.example.order.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom business metrics configuration
 */
@Configuration
public class MetricsConfig {

    // Atomic counters for real-time metrics
    private final AtomicLong ordersCreatedToday = new AtomicLong(0);
    private final AtomicLong ordersCancelledToday = new AtomicLong(0);
    private final AtomicLong orderProcessingErrors = new AtomicLong(0);
    private final AtomicLong totalRevenue = new AtomicLong(0);

    @Bean
    public Counter orderCreatedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("orders.created.total")
                .description("Total number of orders created")
                .tag("service", "order")
                .register(meterRegistry);
    }

    @Bean
    public Counter orderCancelledCounter(MeterRegistry meterRegistry) {
        return Counter.builder("orders.cancelled.total")
                .description("Total number of orders cancelled")
                .tag("service", "order")
                .register(meterRegistry);
    }

    @Bean
    public Counter orderProcessingErrorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("orders.processing.errors.total")
                .description("Total number of order processing errors")
                .tag("service", "order")
                .register(meterRegistry);
    }

    @Bean
    public Timer orderProcessingTimer(MeterRegistry meterRegistry) {
        return Timer.builder("orders.processing.duration")
                .description("Order processing duration")
                .tag("service", "order")
                .register(meterRegistry);
    }

    @Bean
    public Counter compensationExecutedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("orders.compensation.executed.total")
                .description("Total number of compensation actions executed")
                .tag("service", "order")
                .register(meterRegistry);
    }

    @Bean
    public Counter paymentProcessedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("payments.processed.total")
                .description("Total number of payments processed")
                .tag("service", "order")
                .register(meterRegistry);
    }

    @Bean
    public Gauge ordersTodayGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("orders.today.count", ordersCreatedToday, AtomicLong::doubleValue)
                .description("Number of orders created today")
                .tag("service", "order")
                .register(meterRegistry);
    }

    @Bean
    public Gauge ordersCancelledTodayGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("orders.cancelled.today.count", ordersCancelledToday, AtomicLong::doubleValue)
                .description("Number of orders cancelled today")
                .tag("service", "order")
                .register(meterRegistry);
    }

    @Bean
    public Gauge orderProcessingErrorsGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("orders.processing.errors.count", orderProcessingErrors, AtomicLong::doubleValue)
                .description("Current number of order processing errors")
                .tag("service", "order")
                .register(meterRegistry);
    }

    @Bean
    public Gauge totalRevenueGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("revenue.total.yen", totalRevenue, AtomicLong::doubleValue)
                .description("Total revenue in yen")
                .tag("service", "order")
                .tag("currency", "JPY")
                .register(meterRegistry);
    }

    // Methods to update metrics
    public void incrementOrdersCreatedToday() {
        ordersCreatedToday.incrementAndGet();
    }

    public void incrementOrdersCancelledToday() {
        ordersCancelledToday.incrementAndGet();
    }

    public void incrementOrderProcessingErrors() {
        orderProcessingErrors.incrementAndGet();
    }

    public void addRevenue(long amount) {
        totalRevenue.addAndGet(amount);
    }

    public void resetDailyCounters() {
        ordersCreatedToday.set(0);
        ordersCancelledToday.set(0);
    }

    // Getters for current values
    public long getOrdersCreatedToday() {
        return ordersCreatedToday.get();
    }

    public long getOrdersCancelledToday() {
        return ordersCancelledToday.get();
    }

    public long getOrderProcessingErrors() {
        return orderProcessingErrors.get();
    }

    public long getTotalRevenue() {
        return totalRevenue.get();
    }
}