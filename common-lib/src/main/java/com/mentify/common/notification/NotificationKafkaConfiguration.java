package com.mentify.common.notification;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

@Configuration
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "mentify.notifications.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
@Profile("!test")
public class NotificationKafkaConfiguration {

    @Bean
    public KafkaTemplate<String, String> notificationKafkaTemplate(
            KafkaProperties kafkaProperties,
            SslBundles sslBundles
    ) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties(sslBundles);
        producerProperties.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(producerProperties);
        return new KafkaTemplate<>(producerFactory);
    }
}
