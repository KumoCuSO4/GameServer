package com.example.GameServer.Config;

import com.example.GameServer.MessagesProto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;

@Configuration
public class KafkaConfig {

    @Autowired
    private ProducerFactory<String, MessagesProto.register_req> producerFactory;
    @Autowired
    private ConsumerFactory<String, MessagesProto.register_res> consumerFactory;

    @Bean
    public ReplyingKafkaTemplate<String, MessagesProto.register_req, MessagesProto.register_res> replyingKafkaTemplate() {
        return new ReplyingKafkaTemplate<>(producerFactory, kafkaMessageListenerContainer());
    }

    @Bean
    public KafkaMessageListenerContainer<String, MessagesProto.register_res> kafkaMessageListenerContainer() {
        ContainerProperties containerProperties = new ContainerProperties("player_register_reply_topic");
        return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    }
}