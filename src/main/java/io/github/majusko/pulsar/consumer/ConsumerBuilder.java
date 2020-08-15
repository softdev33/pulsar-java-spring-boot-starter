package io.github.majusko.pulsar.consumer;

import io.github.majusko.pulsar.collector.ConsumerCollector;
import io.github.majusko.pulsar.collector.ConsumerHolder;
import io.github.majusko.pulsar.error.FailedMessage;
import io.github.majusko.pulsar.error.exception.ConsumerInitException;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

@Component
@DependsOn({"pulsarClient", "consumerCollector"})
public class ConsumerBuilder {

    private final EmitterProcessor<FailedMessage> exceptionEmitter = EmitterProcessor.create();
    private final ConsumerCollector consumerCollector;
    private final PulsarClient pulsarClient;

    private List<Consumer> consumers;

    public ConsumerBuilder(ConsumerCollector consumerCollector, PulsarClient pulsarClient) {
        this.consumerCollector = consumerCollector;
        this.pulsarClient = pulsarClient;
    }

    @PostConstruct
    private void init() {
        consumers = consumerCollector.getConsumers().entrySet().stream()
            .map(holder -> subscribe(holder.getKey(), holder.getValue()))
            .collect(Collectors.toList());
    }

    private Consumer<?> subscribe(String name, ConsumerHolder holder) {
        try {
            return pulsarClient
                .newConsumer(Schema.JSON(holder.getAnnotation().clazz()))
                .consumerName("consumer-" + name)
                .subscriptionName("subscription-" + name)
                .topic(holder.getAnnotation().topic())
                .subscriptionType(holder.getAnnotation().subscriptionType())
                .messageListener((consumer, msg) -> {
                    try {
                        final Method method = holder.getHandler();

                        method.setAccessible(true);
                        method.invoke(holder.getBean(), msg.getValue());

                        consumer.acknowledge(msg);
                    } catch (Exception e) {
                        consumer.negativeAcknowledge(msg);
                        exceptionEmitter.onNext(new FailedMessage(e, consumer, msg));
                    }
                }).subscribe();
        } catch (PulsarClientException e) {
            throw new ConsumerInitException("Failed to init consumer.", e);
        }
    }

    public List<Consumer> getConsumers() {
        return consumers;
    }

    public Disposable onError(java.util.function.Consumer<? super FailedMessage> consumer) {
        return exceptionEmitter.subscribe(consumer);
    }
}
