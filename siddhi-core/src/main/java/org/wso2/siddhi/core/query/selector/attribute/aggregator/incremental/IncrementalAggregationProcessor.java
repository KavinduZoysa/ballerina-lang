package org.wso2.siddhi.core.query.selector.attribute.aggregator.incremental;

import org.wso2.siddhi.core.event.ComplexEvent;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.stream.MetaStreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventPool;
import org.wso2.siddhi.core.exception.SiddhiAppCreationException;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.query.processor.Processor;

import java.util.List;

/**
 * Incremental Aggregation Processor to consume events to Incremental Aggregators
 */
public class IncrementalAggregationProcessor implements Processor {
    private final List<ExpressionExecutor> incomingExpressionExecutors;
    private final MetaStreamEvent processedMetaStreamEvent;
    private final StreamEventPool streamEventPool;
    private IncrementalExecutor incrementalExecutor;

    public IncrementalAggregationProcessor(IncrementalExecutor incrementalExecutor,
                                           List<ExpressionExecutor> incomingExpressionExecutors,
                                           MetaStreamEvent processedMetaStreamEvent) {
        this.incrementalExecutor = incrementalExecutor;
        this.incomingExpressionExecutors = incomingExpressionExecutors;
        this.processedMetaStreamEvent = processedMetaStreamEvent;
        this.streamEventPool = new StreamEventPool(processedMetaStreamEvent, 5);
    }

    @Override
    public void process(ComplexEventChunk complexEventChunk) {
        ComplexEventChunk<StreamEvent> streamEventChunk =
                new ComplexEventChunk<>(complexEventChunk.isBatch());
        while (complexEventChunk.hasNext()) {
            ComplexEvent complexEvent = complexEventChunk.next();
            StreamEvent borrowedEvent = streamEventPool.borrowEvent();
            for (int i = 0; i < incomingExpressionExecutors.size(); i++) {
                ExpressionExecutor expressionExecutor = incomingExpressionExecutors.get(i);
                borrowedEvent.setOutputData(expressionExecutor.execute(complexEvent), i);
            }
            streamEventChunk.add(borrowedEvent);
        }
        incrementalExecutor.execute(streamEventChunk);
    }

    @Override
    public Processor getNextProcessor() {
        return null;
    }

    @Override
    public void setNextProcessor(Processor processor) {
        throw new SiddhiAppCreationException("IncrementalAggregationProcessor does not support any next processor");
    }

    @Override
    public void setToLast(Processor processor) {
        throw new SiddhiAppCreationException("IncrementalAggregationProcessor does not support any " +
                "next/last processor");
    }

    @Override
    public Processor cloneProcessor(String key) {
        throw new SiddhiAppCreationException("IncrementalAggregationProcessor cannot be cloned");
    }
}
