package com.pronurse.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;

/**
 * Test configuration to provide mock beans for testing
 */
@TestConfiguration
@Import(TestFlywayConfig.class)
public class TestConfig {

    /**
     * Provides a mock SimpMessagingTemplate for WebSocket testing
     * This prevents WebSocket configuration issues in tests
     */
    @Bean
    @Primary
    public SimpMessagingTemplate simpMessagingTemplate() {
        return mock(SimpMessagingTemplate.class);
    }
}
