package com.paypal.notification_service.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();
        // support Java 8+ time types like Instant
        om.registerModule(new JavaTimeModule());
        // use ISO dates instead of timestamps
        om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // be permissive for unknown properties in incoming JSON
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return om;
    }
}

