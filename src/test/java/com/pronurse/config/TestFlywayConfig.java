package com.pronurse.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for Flyway
 * Flyway is auto-configured by Spring Boot and will run migrations automatically
 */
@TestConfiguration
@Profile("test")
public class TestFlywayConfig {
    // No custom beans needed - Spring Boot auto-configuration handles Flyway
    // The application-test.properties file configures Flyway properly
}