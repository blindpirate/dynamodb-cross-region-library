package com.amazonaws.services.dynamodbv2.streams.connectors.composite;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class CompositeRecordProcessorFactory implements IRecordProcessorFactory {
    private final List<? extends IRecordProcessorFactory> delegates;

    public CompositeRecordProcessorFactory(List<? extends IRecordProcessorFactory> delegates) {
        this.delegates = delegates;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return new CompositeRecordProcessor(
                delegates
                        .stream()
                        .map(IRecordProcessorFactory::createProcessor)
                        .collect(Collectors.toList())
        );
    }
}

class CompositeRecordProcessor implements IRecordProcessor {
    private final ExecutorService threadPool;
    private final List<IRecordProcessor> processors;

    public CompositeRecordProcessor(List<IRecordProcessor> processors) {
        this.processors = processors;
        this.threadPool = Executors.newFixedThreadPool(processors.size());
    }

    @Override
    public void initialize(String shardId) {
        processors.forEach(it -> it.initialize(shardId));
    }

    @Override
    public void processRecords(List<Record> records, IRecordProcessorCheckpointer checkpointer) {
        for (IRecordProcessor processor : processors) {
            threadPool.submit(() -> processor.processRecords(records, checkpointer));
        }
    }

    @Override
    public void shutdown(IRecordProcessorCheckpointer checkpointer, ShutdownReason reason) {
        processors.forEach(it -> it.shutdown(checkpointer, reason));
    }
}

