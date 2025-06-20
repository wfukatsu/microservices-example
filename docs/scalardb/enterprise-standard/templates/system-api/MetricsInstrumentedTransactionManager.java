package com.example.scalardb.systemapi.config;

import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.exception.storage.ExecutionException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

public class MetricsInstrumentedTransactionManager implements DistributedTransactionManager {

    private final DistributedTransactionManager delegate;
    private final Timer transactionTimer;
    private final Counter transactionSuccessCounter;
    private final Counter transactionFailureCounter;
    private final Counter transactionAbortCounter;

    public MetricsInstrumentedTransactionManager(DistributedTransactionManager delegate, 
                                               MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.transactionTimer = Timer.builder("scalardb.transaction.duration")
                .description("Transaction execution time")
                .register(meterRegistry);
        this.transactionSuccessCounter = Counter.builder("scalardb.transaction.success")
                .description("Number of successful transactions")
                .register(meterRegistry);
        this.transactionFailureCounter = Counter.builder("scalardb.transaction.failure")
                .description("Number of failed transactions")
                .register(meterRegistry);
        this.transactionAbortCounter = Counter.builder("scalardb.transaction.abort")
                .description("Number of aborted transactions")
                .register(meterRegistry);
    }

    @Override
    public DistributedTransaction start() throws ExecutionException {
        DistributedTransaction transaction = delegate.start();
        return new MetricsInstrumentedTransaction(transaction, this);
    }

    @Override
    public DistributedTransaction start(String transactionId) throws ExecutionException {
        DistributedTransaction transaction = delegate.start(transactionId);
        return new MetricsInstrumentedTransaction(transaction, this);
    }

    @Override
    public void close() {
        delegate.close();
    }

    public void recordTransactionSuccess() {
        transactionSuccessCounter.increment();
    }

    public void recordTransactionFailure() {
        transactionFailureCounter.increment();
    }

    public void recordTransactionAbort() {
        transactionAbortCounter.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(transactionTimer);
    }
}