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
        props.put("security.protocol", "SSL");
        // SSL setup using PEM files located on the classpath
        Resource caPemResource = new ClassPathResource(truststoreResource);
        if (caPemResource.exists()) {
            File caTempFile = createTempFileFromResource(caPemResource, "kafka-ca", ".pem");
            props.put("ssl.truststore.location", caTempFile.getAbsolutePath());
            props.put("ssl.truststore.type", "PEM");
        }
        Resource svcPemResource = new ClassPathResource(keystoreResource);
        if (svcPemResource.exists()) {
            File svcTempFile = createTempFileFromResource(svcPemResource, "kafka-svc", ".pem");
            props.put("ssl.keystore.location", svcTempFile.getAbsolutePath());
            props.put("ssl.keystore.type", "PEM");
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

    private File createTempFileFromResource(Resource resource, String prefix, String suffix) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            File tempFile = File.createTempFile(prefix, suffix);
            tempFile.deleteOnExit(); // Delete on JVM shutdown
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        }
    }

}
