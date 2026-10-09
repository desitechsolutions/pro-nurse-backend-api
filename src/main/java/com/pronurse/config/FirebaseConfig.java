package com.pronurse.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Profile("!test")  // Skip Firebase initialization in test profile
@Slf4j
public class FirebaseConfig {

    @Value("${fcm.credentials.path:classpath:pro-nurse-backend-api-firebase-adminsdk-fbsvc-333ca6a8b0.json}")
    private Resource firebaseCredentials;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                if (firebaseCredentials != null && firebaseCredentials.exists()) {
                    try (InputStream serviceAccount = firebaseCredentials.getInputStream()) {
                        FirebaseOptions options = FirebaseOptions.builder()
                                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                .build();

                        FirebaseApp.initializeApp(options);
                        log.info("Firebase Admin SDK initialized successfully from: {}", firebaseCredentials.getFilename());
                    }
                } else {
                    log.warn("Firebase credentials resource not found at: {}. FCM push notifications will be disabled.", firebaseCredentials);
                }
            } else {
                log.info("Firebase Admin SDK is already initialized.");
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin SDK", e);
            log.warn("FCM push notifications will not be available. Please verify the service account JSON key.");
        } catch (Exception e) {
            log.error("Unexpected error initializing Firebase", e);
        }
    }
}
