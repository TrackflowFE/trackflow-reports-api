package com.trackflow.bootstrap.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public record SecurityProperties(boolean enabled, String secret, Duration inactivity) {
    @Autowired
    public SecurityProperties(
            @Value("${trackflow.security.enabled:true}") boolean enabled,
            @Value("${trackflow.security.jwt-secret:}") String secret,
            @Value("${trackflow.security.inactividad-minutos:30}") long inactivityMinutes) {
        this(enabled, secret, Duration.ofMinutes(inactivityMinutes));
        if (enabled && secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("TRACKFLOW_JWT_SECRET debe tener al menos 32 bytes y coincidir con API 1");
        }
        if (inactivityMinutes <= 0) {
            throw new IllegalArgumentException("TRACKFLOW_INACTIVIDAD_MINUTOS debe ser positivo");
        }
    }
}
