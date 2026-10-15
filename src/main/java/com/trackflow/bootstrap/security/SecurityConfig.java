package com.trackflow.bootstrap.security;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SecurityProperties properties) {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers("/", "/error", "/actuator/health/**", "/swagger-ui.html",
                            "/swagger-ui/**", "/v3/api-docs/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/tracking/**").permitAll();
                    if (properties.enabled()) {
                        auth.requestMatchers(HttpMethod.GET, "/api/reports/**").hasAnyRole("OPERADOR", "ADMIN");
                        auth.anyRequest().denyAll();
                    } else {
                        auth.requestMatchers(HttpMethod.GET, "/api/reports/**").permitAll();
                        auth.anyRequest().denyAll();
                    }
                });
        if (properties.enabled()) {
            var authorities = new JwtGrantedAuthoritiesConverter();
            authorities.setAuthoritiesClaimName("roles");
            authorities.setAuthorityPrefix("ROLE_");
            var converter = new JwtAuthenticationConverter();
            converter.setJwtGrantedAuthoritiesConverter(authorities);
            http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
        } else {
            LoggerFactory.getLogger(SecurityConfig.class).warn(
                    "TRACKFLOW_SECURITY_ENABLED=false: solo para desarrollo local, nunca en Render");
        }
        return http.build();
    }
}
