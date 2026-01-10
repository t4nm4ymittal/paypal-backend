package com.paypal.notification_service.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Value("${spring.kafka.bootstrap-servers:kafka-1837b1b2-mnit-ed33.d.aivencloud.com:21285}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:notification-service-group}")
    private String groupId;

    @Value("${kafka.ssl.truststore:ca.pem}")
    private String truststoreResource;

    @Value("${kafka.ssl.keystore:svc.pem}")
    private String keystoreResource;

    @Value("${kafka.debug.unique-group:false}")
    private boolean uniqueGroup;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() throws IOException {
        Map<String, Object> props = new HashMap<>();

        // compute effective group id (allow debug override to force new group)
        String effectiveGroupId = groupId;
        if (uniqueGroup) {
            effectiveGroupId = groupId + "-" + System.currentTimeMillis();
        }

        // basic connection settings
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, effectiveGroupId);

        // consumer behavior
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        // SSL setup using PEM files located on the classpath
        ClassPathResource trust = new ClassPathResource(truststoreResource);
        ClassPathResource key = new ClassPathResource(keystoreResource);

        // resolve to absolute paths if available
        if (trust.exists() && key.exists()) {
            props.put("security.protocol", "SSL");
            props.put("ssl.truststore.location", trust.getFile().getAbsolutePath());
            props.put("ssl.truststore.type", "PEM");
            props.put("ssl.keystore.location", key.getFile().getAbsolutePath());
            props.put("ssl.keystore.type", "PEM");
        } else {
            logger.warn("Kafka SSL resources not found on classpath: {} {}, proceeding without ssl file paths (check config)", truststoreResource, keystoreResource);
        }

        logger.info("Kafka consumer factory configured: bootstrapServers={} groupId={} (effective={})", bootstrapServers, groupId, effectiveGroupId);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() throws IOException {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        return factory;
    }
}
