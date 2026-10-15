package com.trackflow.bootstrap.security;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Lectura de referencia: API 1 gobierna usuarios, sesiones y su actividad. */
@Component
public class SharedSessionValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID_SESSION = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
            "La sesion fue cerrada, vencio o el usuario esta inactivo", null);

    private final JdbcTemplate jdbc;
    private final SecurityProperties properties;
    private final Clock clock;

    public SharedSessionValidator(JdbcTemplate jdbc, SecurityProperties properties, Clock clock) {
        this.jdbc = jdbc;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (token.getId() == null || token.getSubject() == null || token.getExpiresAt() == null) {
            return OAuth2TokenValidatorResult.failure(INVALID_SESSION);
        }
        Instant now = clock.instant();
        Timestamp inactivityCutoff = Timestamp.from(now.minus(properties.inactivity()));
        Timestamp current = Timestamp.from(now);
        Boolean active = jdbc.queryForObject("""
                select exists (
                    select 1 from auth_sessions s join auth_users u on u.username = s.username
                    where s.jti = ? and s.username = ? and s.closed_at is null
                      and s.expires_at > ? and s.last_activity_at >= ? and u.active = true
                )
                """, Boolean.class, token.getId(), token.getSubject(), current, inactivityCutoff);
        return Boolean.TRUE.equals(active) ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(INVALID_SESSION);
    }
}
