package com.votosync.vote.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String VOTE_QUEUE = "vote-ingestion-queue";
    public static final String VOTE_EXCHANGE = "vote.exchange";
    public static final String VOTE_ROUTING_KEY = "vote.register";

    @Bean
    public Queue voteQueue() {
        // Queue is durable so messages survive broker restarts
        return QueueBuilder.durable(VOTE_QUEUE).build();
    }

    @Bean
    public TopicExchange voteExchange() {
        return new TopicExchange(VOTE_EXCHANGE);
    }

    @Bean
    public Binding binding(Queue voteQueue, TopicExchange voteExchange) {
        return BindingBuilder.bind(voteQueue).to(voteExchange).with(VOTE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
