package com.example.eventapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.awspring.cloud.sqs.annotation.SqsListener;
import software.amazon.awssdk.services.sqs.model.Message;

@Component
class EventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @SqsListener("${sqs.my-queue.name}")
    public void receiveMessage(Message message) {
        logger.info("Received message: {}", message.messageId());
        logger.info(message.body());
    }
}
