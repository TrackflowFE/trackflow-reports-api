package com.trackflow.modules.reports.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReportsRabbitMQConfig {

    @Bean
    MessageConverter integrationEventConverter() {
        return new JacksonJsonMessageConverter("com.trackflow.shared.events");
    }

    @Bean
    Declarables reportsEventTopology(
            @Value("${trackflow.events.exchange}") String exchangeName,
            @Value("${trackflow.events.shipment-created-key}") String shipmentKey,
            @Value("${trackflow.events.logistics-registered-key}") String logisticsKey,
            @Value("${trackflow.reports.events.shipment-created-queue}") String shipmentQueueName,
            @Value("${trackflow.reports.events.logistics-registered-queue}") String logisticsQueueName,
            @Value("${trackflow.reports.events.dead-letter-exchange}") String deadLetterExchangeName,
            @Value("${trackflow.reports.events.dead-letter-queue}") String deadLetterQueueName) {
        TopicExchange exchange = new TopicExchange(exchangeName, true, false);
        TopicExchange deadLetterExchange = new TopicExchange(deadLetterExchangeName, true, false);
        Queue shipments = QueueBuilder.durable(shipmentQueueName)
                .deadLetterExchange(deadLetterExchangeName).deadLetterRoutingKey(shipmentKey).build();
        Queue logistics = QueueBuilder.durable(logisticsQueueName)
                .deadLetterExchange(deadLetterExchangeName).deadLetterRoutingKey(logisticsKey).build();
        Queue deadLetters = QueueBuilder.durable(deadLetterQueueName).build();
        Binding shipmentBinding = BindingBuilder.bind(shipments).to(exchange).with(shipmentKey);
        Binding logisticsBinding = BindingBuilder.bind(logistics).to(exchange).with(logisticsKey);
        Binding deadLetterBinding = BindingBuilder.bind(deadLetters).to(deadLetterExchange).with("#");
        return new Declarables(exchange, deadLetterExchange, shipments, logistics, deadLetters,
                shipmentBinding, logisticsBinding, deadLetterBinding);
    }
}
