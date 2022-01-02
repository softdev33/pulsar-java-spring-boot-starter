package io.github.majusko.pulsar;

import io.github.majusko.pulsar.error.exception.ClientInitException;
import io.github.majusko.pulsar.msg.MyMsg;
import io.github.majusko.pulsar.reactor.FluxConsumer;
import io.github.majusko.pulsar.reactor.FluxConsumerFactory;
import io.github.majusko.pulsar.reactor.PulsarFluxConsumer;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestFluxConsumersConfiguration {

    public static final String BASIC_FLUX_TOPIC_TEST = "basic-flux-test-topic";

    @Autowired
    private FluxConsumerFactory fluxConsumerFactory;

    @Bean
    public FluxConsumer<MyMsg> myTestFluxConsumer() throws ClientInitException, PulsarClientException {
        return fluxConsumerFactory.newConsumer(
            PulsarFluxConsumer.builder()
                .setTopic(BASIC_FLUX_TOPIC_TEST)
                .setConsumerName("my-consumer-name")
                .setSubscriptionName("my-subscription-name")
                .setClazz(MyMsg.class)
                .build());
    }
}
