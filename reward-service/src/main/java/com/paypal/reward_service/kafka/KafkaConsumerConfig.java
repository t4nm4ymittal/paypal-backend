package com.paypal.reward_service.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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

    @Value("${kafka.ssl.key-password:}")
    private String keyPassword;

    @Value("${kafka.debug.unique-group:false}")
    private boolean uniqueGroup;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() throws IOException {

        Map<String, Object> props = new HashMap<>();

        String effectiveGroupId = uniqueGroup
                ? groupId + "-" + System.currentTimeMillis()
                : groupId;

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, effectiveGroupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        // ---------- SSL (Aiven / Cloud Safe) ----------
        props.put("security.protocol", "SSL");
        props.put("ssl.endpoint.identification.algorithm", "");

        // CA
        Resource caPem = new ClassPathResource(truststoreResource);
        File caFile = createTempFile(caPem, "kafka-ca", ".pem");
        props.put("ssl.truststore.location", caFile.getAbsolutePath());
        props.put("ssl.truststore.type", "PEM");

        // Client cert
        Resource svcPem = new ClassPathResource(keystoreResource);
        File svcFile = createTempFile(svcPem, "kafka-svc", ".pem");
        props.put("ssl.keystore.location", svcFile.getAbsolutePath());
        props.put("ssl.keystore.type", "PEM");

        // Private key password (required by Aiven)
        if (!keyPassword.isEmpty()) {
            props.put("ssl.key.password", keyPassword);
        }

        logger.info("Kafka consumer configured | bootstrap={} | group={}",
                bootstrapServers, effectiveGroupId);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    kafkaListenerContainerFactory() throws IOException {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        return factory;
    }

    private File createTempFile(Resource resource, String prefix, String suffix) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            File temp = File.createTempFile(prefix, suffix);
            temp.deleteOnExit();
            Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return temp;
        }
    }
}
