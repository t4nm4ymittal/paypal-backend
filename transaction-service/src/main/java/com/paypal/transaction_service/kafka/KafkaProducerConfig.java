package com.paypal.transaction_service.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, String> producerFactory() throws IOException {
        Map<String, Object> configProps = new HashMap<>();
        ClassPathResource capem = new ClassPathResource("ca.pem");
        ClassPathResource svcpem = new ClassPathResource("svc.pem");
        // Kafka bootstrap servers
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-1837b1b2-mnit-ed33.d.aivencloud.com:21285");

        // Serializer settings
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // SSL settings for Aiven
        configProps.put("security.protocol", "SSL");
        configProps.put("ssl.truststore.location", capem.getFile().getAbsolutePath());
        // replace with your password
        configProps.put("ssl.truststore.type", "PEM");

        configProps.put("ssl.keystore.location", svcpem.getFile().getAbsolutePath());
     // replace with your password
        configProps.put("ssl.keystore.type", "PEM");



        // Optional: additional producer configs
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() throws IOException {
        return new KafkaTemplate<>(producerFactory());
    }
}
