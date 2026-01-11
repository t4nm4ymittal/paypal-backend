package com.paypal.transaction_service.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:kafka-1837b1b2-mnit-ed33.d.aivencloud.com:21285}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, String> producerFactory() throws IOException {
        Map<String, Object> configProps = new HashMap<>();

        // Kafka bootstrap servers
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Serializer settings
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // SSL settings for Aiven
        configProps.put("security.protocol", "SSL");

        // ✅ Load ca.pem from classpath and copy to temp file
        Resource caPemResource = new ClassPathResource("ca.pem");
        if (caPemResource.exists()) {
            File caTempFile = createTempFileFromResource(caPemResource, "kafka-ca", ".pem");
            configProps.put("ssl.truststore.location", caTempFile.getAbsolutePath());
            configProps.put("ssl.truststore.type", "PEM");
        }

        // ✅ Load svc.pem from classpath and copy to temp file
        Resource svcPemResource = new ClassPathResource("svc.pem");
        if (svcPemResource.exists()) {
            File svcTempFile = createTempFileFromResource(svcPemResource, "kafka-svc", ".pem");
            configProps.put("ssl.keystore.location", svcTempFile.getAbsolutePath());
            configProps.put("ssl.keystore.type", "PEM");
        }

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

    /**
     * Helper method to create a temporary file from a classpath resource
     */
    private File createTempFileFromResource(Resource resource, String prefix, String suffix) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            File tempFile = File.createTempFile(prefix, suffix);
            tempFile.deleteOnExit(); // Delete on JVM shutdown
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        }
    }
}