package com.trackflow.bootstrap.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Límite de consultas al rastreo público. El número de seguimiento es difícil de
 * adivinar (16^12 combinaciones), así que esto es una segunda barrera contra la
 * enumeración y los sondeos masivos.
 *
 * Cada petición cuenta y no se libera: el límite es de consultas, no de fallos. Con
 * X-Forwarded-For sin configurar, la IP del proxy es la que se cuenta, igual que en el
 * autoservicio; por eso los proxies confiables se configuran con IP o rango CIDR.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class LimiteDeRastreoFilter extends OncePerRequestFilter {
    private static final String BASE = "/api/tracking";
    private static final int MAX_CLAVES = 10000;

    private final VentanasDeIntentos ventanas;
    private final IpDelCliente ipDelCliente;
    private final int limite;
    private final long ventanaMillis;
    private final CorsConfiguration cors;

    public LimiteDeRastreoFilter(@Value("${trackflow.tracking.limite.ip:60}") int limite,
            @Value("${trackflow.tracking.limite.ventana-segundos:60}") long segundos, Clock clock,
            IpDelCliente ipDelCliente,
            @Value("${trackflow.cors.allowed-origins:http://localhost:5173}") String origenes) {
        if (limite < 1 || segundos < 1 || segundos > 86400) {
            throw new IllegalArgumentException(
                    "El límite de rastreo debe ser positivo y la ventana de hasta 86400 segundos");
        }
        this.limite = limite;
        this.ventanaMillis = segundos * 1000;
        this.ventanas = new VentanasDeIntentos(MAX_CLAVES, ventanaMillis, clock);
        this.ipDelCliente = ipDelCliente;
        this.cors = new CorsConfiguration();
        this.cors.setAllowedOrigins(Arrays.stream(origenes.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .toList());
        this.cors.setAllowedMethods(List.of("GET", "OPTIONS"));
        this.cors.setAllowedHeaders(List.of("*"));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !"GET".equals(request.getMethod()) || !(path.equals(BASE) || path.startsWith(BASE + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = ipDelCliente.obtener(request);
        if (ventanas.reservar(ip, limite) != null) {
            chain.doFilter(request, response);
            return;
        }
        if (!new DefaultCorsProcessor().processRequest(cors, request, response)) {
            return;
        }
        long espera = Math.max(1, ventanaMillis / 1000);
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(espera));
        response.setHeader("Cache-Control", "no-store");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/problem+json");
        response.getWriter().write(
                "{\"title\":\"Demasiadas consultas\",\"status\":429,\"detail\":\"Superaste el límite de "
                        + "consultas de rastreo; intenta de nuevo en " + espera + " segundos\"}");
    }
}
