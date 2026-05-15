package com.example.ai_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for AI Service
 * Handles quota reset events and scheduling
 */
@Configuration
public class RabbitMqConfig {

    // Exchange for quota reset events
    @Bean
    public TopicExchange quotaEventsExchange(
            @Value("${app.rabbit.quota-events.exchange:quota.events.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    // Queue for quota reset events
    @Bean
    public Queue quotaResetQueue(
            @Value("${app.rabbit.quota-reset.queue:quota.reset.queue}") String queueName) {
        return new Queue(queueName, true, false, false);
    }

    // Binding for quota reset queue
    @Bean
    public Binding quotaResetBinding(Queue quotaResetQueue,
                                     TopicExchange quotaEventsExchange,
                                     @Value("${app.rabbit.quota-reset.routing-key:quota.reset}") String routingKey) {
        return BindingBuilder.bind(quotaResetQueue).to(quotaEventsExchange).with(routingKey);
    }

    // JSON message converter
    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // RabbitTemplate for sending messages
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }
}
