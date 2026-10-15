package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "trackflow.security.enabled=true",
        "trackflow.security.jwt-secret=reports-test-secret-not-for-production-123456789"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportsSecurityTest {
    private static final String SECRET = "reports-test-secret-not-for-production-123456789";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void session() {
        jdbc.update("delete from auth_sessions");
        jdbc.update("delete from auth_users");
        jdbc.update("insert into auth_users(username, active) values ('operator', true)");
        jdbc.update("insert into auth_sessions(jti, username, expires_at, last_activity_at) values (?, ?, ?, ?)",
                "session-test", "operator", Timestamp.from(Instant.now().plusSeconds(3600)),
                Timestamp.from(Instant.now()));
    }

    @Test
    void publicTrackingAndHealthDoNotRequireTokenButReportsDo() throws Exception {
        mvc.perform(get("/api/tracking/UNKNOWN")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/api/reports/delayed-shipments")).andExpect(status().isUnauthorized());
    }

    @Test
    void operatorAndAdministratorCanReadReports() throws Exception {
        mvc.perform(get("/api/reports/delayed-shipments").header("Authorization", token("OPERADOR")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/reports/delayed-shipments").header("Authorization", token("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/reports/delayed-shipments").header("Authorization", token("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void closedInactiveAndExpiredSessionsAreRejected() throws Exception {
        String token = token("OPERADOR");
        jdbc.update("update auth_sessions set closed_at = ?", Timestamp.from(Instant.now()));
        mvc.perform(get("/api/reports/delayed-shipments").header("Authorization", token))
                .andExpect(status().isUnauthorized());
        session();
        jdbc.update("update auth_users set active = false");
        mvc.perform(get("/api/reports/delayed-shipments").header("Authorization", token))
                .andExpect(status().isUnauthorized());
        session();
        jdbc.update("update auth_sessions set last_activity_at = ?", Timestamp.from(Instant.now().minusSeconds(1900)));
        mvc.perform(get("/api/reports/delayed-shipments").header("Authorization", token))
                .andExpect(status().isUnauthorized());
        session();
        jdbc.update("update auth_sessions set expires_at = ?", Timestamp.from(Instant.now().minusSeconds(1)));
        mvc.perform(get("/api/reports/delayed-shipments").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongSecretExpiredJwtAndMismatchedSubjectAreRejected() throws Exception {
        mvc.perform(get("/api/reports/delayed-shipments").header("Authorization",
                token("OPERADOR", SECRET + "wrong", "operator", Instant.now().plusSeconds(3600))))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reports/delayed-shipments").header("Authorization",
                token("OPERADOR", SECRET, "operator", Instant.now().minusSeconds(120))))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reports/delayed-shipments").header("Authorization",
                token("OPERADOR", SECRET, "other-user", Instant.now().plusSeconds(3600))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsAllowsFrontendPreflightAndRejectsUnknownOrigin() throws Exception {
        mvc.perform(options("/api/reports/delayed-shipments").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET").header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/reports/delayed-shipments").header("Origin", "https://unknown.example")
                .header("Access-Control-Request-Method", "GET")).andExpect(status().isForbidden());
    }

    @Test
    void loginAndWritesDoNotBelongToThisApi() throws Exception {
        mvc.perform(post("/api/auth/login").header("Authorization", token("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/shipments").header("Authorization", token("OPERADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void secureConfigurationRequiresSharedSecret() {
        assertThrows(IllegalArgumentException.class, () -> new SecurityProperties(true, "short", 30L));
        assertDoesNotThrow(() -> new SecurityProperties(false, "", 30L));
    }

    private String token(String roles) {
        return token(roles, SECRET, "operator", Instant.now().plusSeconds(3600));
    }

    private String token(String roles, String secret, String subject, Instant expiresAt) {
        var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var claims = JwtClaimsSet.builder().id("session-test").issuer("trackflow").subject(subject)
                .issuedAt(Instant.now().minusSeconds(3600)).expiresAt(expiresAt).claim("roles", roles).build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
