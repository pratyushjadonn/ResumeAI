package com.example.export_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange resumeAiExchange(
            @Value("${app.rabbitmq.exchange:resumeai.exchange}") String exchangeName) {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Queue notificationQueue(
            @Value("${app.rabbitmq.notification-queue:notification.queue}") String queueName) {
        return new Queue(queueName);
    }

    @Bean
    public Queue exportQueue(
            @Value("${app.rabbitmq.export-queue:export.queue}") String queueName) {
        return new Queue(queueName);
    }

    @Bean
    public Binding notificationBinding(
            @Qualifier("notificationQueue") Queue notificationQueue,
            DirectExchange resumeAiExchange,
            @Value("${app.rabbitmq.notification-routing-key:notification.routing}") String routingKey) {
        return BindingBuilder.bind(notificationQueue).to(resumeAiExchange).with(routingKey);
    }

    @Bean
    public Binding exportBinding(
            @Qualifier("exportQueue") Queue exportQueue,
            DirectExchange resumeAiExchange,
            @Value("${app.rabbitmq.export-routing-key:export.routing}") String routingKey) {
        return BindingBuilder.bind(exportQueue).to(resumeAiExchange).with(routingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }
}
