package com.wethinkcode.hrsystem.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;


@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "hr.events";
    public static final String NOTIFICATION_QUEUE = "notification.queue";

    @Bean
    public TopicExchange hrEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true); // durable
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange hrEventsExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(hrEventsExchange)
                .with("*.scheduled");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("com.wethinkcode.hrsystem.dto");
        // or, for everything under your base package:
        // typeMapper.setTrustedPackages("com.wethinkcode.hrsystem.*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public Binding notificationOutcomeBinding(Queue notificationQueue, TopicExchange hrEventsExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(hrEventsExchange)
                .with("#.changed");
    }

    @Bean
    public Binding notificationInviteBinding(Queue notificationQueue, TopicExchange hrEventsExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(hrEventsExchange)
                .with("#.invited");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory factory =
                new org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}