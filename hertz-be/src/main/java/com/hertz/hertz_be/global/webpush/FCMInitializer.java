package com.hertz.hertz_be.global.webpush;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class FCMInitializer {

    @Value("${fcm.certification}")
    private String fcmCredentials;

    @PostConstruct
    public void initialize() {
        try (InputStream is = resolveInputStream(fcmCredentials)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(is))
                    .build();

            if (FirebaseApp.getApps().stream().noneMatch(app -> app.getName().equals(FirebaseApp.DEFAULT_APP_NAME))) {
                FirebaseApp.initializeApp(options);
                log.info("🔥 FirebaseApp initialization SUCCESS");
            } else {
                log.info("✅ FirebaseApp already initialized");
            }
        } catch (Exception e) {
            log.error("❌ FCM initialization FAIL", e);
        }
    }

    private InputStream resolveInputStream(String path) throws IOException {
        log.info("📂 Loading FCM credentials from path: {}", path);
        return new FileInputStream(path);
    }
}
