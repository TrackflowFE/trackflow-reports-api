package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LimiteDeRastreoFilterTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);

    private MockHttpServletRequest peticion(String ruta, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", ruta);
        request.setServletPath(ruta);
        request.setRemoteAddr(ip);
        return request;
    }

    private MockHttpServletResponse consultar(LimiteDeRastreoFilter filtro, String ip, String xff) throws Exception {
        MockHttpServletRequest request = peticion("/api/tracking/TRK-1", ip);
        if (xff != null) request.addHeader("X-Forwarded-For", xff);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void responde429AlSuperarElLimite() throws Exception {
        LimiteDeRastreoFilter filtro = new LimiteDeRastreoFilter(2, 60, RELOJ, new IpDelCliente(0, ""),
                "http://localhost:5173");

        assertEquals(200, consultar(filtro, "192.0.2.1", null).getStatus());
        assertEquals(200, consultar(filtro, "192.0.2.1", null).getStatus());
        assertEquals(429, consultar(filtro, "192.0.2.1", null).getStatus());
    }

    @Test
    void el429LlevaRetryAfterYNoSeCachea() throws Exception {
        LimiteDeRastreoFilter filtro = new LimiteDeRastreoFilter(1, 45, RELOJ, new IpDelCliente(0, ""),
                "http://localhost:5173");
        consultar(filtro, "192.0.2.1", null);

        MockHttpServletResponse response = consultar(filtro, "192.0.2.1", null);

        assertEquals(429, response.getStatus());
        assertEquals("45", response.getHeader("Retry-After"));
        assertEquals("no-store", response.getHeader("Cache-Control"));
    }

    @Test
    void direccionesDistintasTienenCuotasSeparadas() throws Exception {
        LimiteDeRastreoFilter filtro = new LimiteDeRastreoFilter(1, 60, RELOJ, new IpDelCliente(0, ""),
                "http://localhost:5173");

        assertEquals(200, consultar(filtro, "192.0.2.1", null).getStatus());
        assertEquals(429, consultar(filtro, "192.0.2.1", null).getStatus());
        assertEquals(200, consultar(filtro, "192.0.2.2", null).getStatus());
    }

    @Test
    void conSaltosEnCeroLaCabeceraNoEvitaElLimite() throws Exception {
        LimiteDeRastreoFilter filtro = new LimiteDeRastreoFilter(1, 60, RELOJ, new IpDelCliente(0, ""),
                "http://localhost:5173");

        assertEquals(200, consultar(filtro, "192.0.2.1", "198.51.100.1").getStatus());
        assertEquals(429, consultar(filtro, "192.0.2.1", "198.51.100.2").getStatus());
    }

    @Test
    void laVentanaSeReiniciaAlCumplirElPlazo() throws Exception {
        class Reloj extends Clock {
            long millis = RELOJ.millis();

            public ZoneOffset getZone() { return ZoneOffset.UTC; }

            public Clock withZone(java.time.ZoneId zone) { return this; }

            public Instant instant() { return Instant.ofEpochMilli(millis); }
        }
        Reloj reloj = new Reloj();
        LimiteDeRastreoFilter filtro = new LimiteDeRastreoFilter(1, 60, reloj, new IpDelCliente(0, ""),
                "http://localhost:5173");

        assertEquals(200, consultar(filtro, "192.0.2.1", null).getStatus());
        assertEquals(429, consultar(filtro, "192.0.2.1", null).getStatus());
        reloj.millis += 60000;
        assertEquals(200, consultar(filtro, "192.0.2.1", null).getStatus());
    }

    @Test
    void noAfectaLasRutasDeReportes() throws Exception {
        LimiteDeRastreoFilter filtro = new LimiteDeRastreoFilter(1, 60, RELOJ, new IpDelCliente(0, ""),
                "http://localhost:5173");

        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filtro.doFilter(peticion("/api/reports/delayed-shipments", "192.0.2.1"), response, new MockFilterChain());
            assertEquals(200, response.getStatus());
        }
    }
}
