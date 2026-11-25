package com.trackflow.bootstrap.security;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SecurityProperties properties,
            @Value("${management.server.port:-1}") int puertoDeGestion) {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // El rastreo público lleva el número de seguimiento en la URL, y ese número
                // es la credencial del cliente: estas cabeceras acotan hasta dónde llega.
                .headers(h -> h
                        // Sin esto, al pulsar un enlace externo el navegador mandaría la URL
                        // completa —número incluido— al sitio de destino en el Referer.
                        .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER))
                        // El navegador recuerda un año que este dominio es solo HTTPS, así que
                        // una petición escrita como http:// no llega a salir en claro.
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .cacheControl(Customizer.withDefaults())
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    // Las métricas de Prometheus solo salen por el puerto de gestión, que
                    // el clúster no publica hacia afuera. Si MANAGEMENT_SERVER_PORT no está
                    // definida (Render), esta regla no se añade y /actuator/prometheus
                    // sigue cayendo en el denyAll de abajo.
                    if (puertoDeGestion > 0) {
                        auth.requestMatchers(peticion -> peticion.getLocalPort() == puertoDeGestion
                                && peticion.getRequestURI().startsWith("/actuator/")).permitAll();
                    }
                    auth.requestMatchers("/", "/error", "/actuator/health/**", "/swagger-ui.html",
                            "/swagger-ui/**", "/v3/api-docs/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/tracking/**").permitAll();
                });
        if (properties.enabled()) {
            // HU-11: el operador registra, no supervisa; los reportes son del supervisor y del admin.
            http.authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET, "/api/reports/**")
                    .hasAnyRole("SUPERVISOR", "ADMIN"));
            var authorities = new JwtGrantedAuthoritiesConverter();
            authorities.setAuthoritiesClaimName("roles");
            authorities.setAuthorityPrefix("ROLE_");
            var converter = new JwtAuthenticationConverter();
            converter.setJwtGrantedAuthoritiesConverter(authorities);
            http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
        } else {
            http.authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET, "/api/reports/**").permitAll());
            LoggerFactory.getLogger(SecurityConfig.class).warn(
                    "TRACKFLOW_SECURITY_ENABLED=false: solo para desarrollo local, nunca en Render");
        }
        http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
        return http.build();
    }
}
