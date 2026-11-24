package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ForwardedHeaderFilter;

class IpDelClienteTest {
    private MockHttpServletRequest request(String peer, String xff) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(peer);
        if (xff != null) request.addHeader("X-Forwarded-For", xff);
        return request;
    }

    @Test
    void soloConfiaEnPeerConfiguradoYUsaLaDerecha() {
        IpDelCliente ips = new IpDelCliente(1, "192.0.2.10");
        assertEquals("198.51.100.2", ips.obtener(request("192.0.2.10", "203.0.113.1, 198.51.100.2")));
        assertEquals("192.0.2.11", ips.obtener(request("192.0.2.11", "198.51.100.2")));
        assertEquals("192.0.2.10", ips.obtener(request("192.0.2.10", null)));
        assertEquals("192.0.2.10", ips.obtener(request("192.0.2.10", "no-es-ip")));
        assertEquals("192.0.2.10", new IpDelCliente(0, "").obtener(request("192.0.2.10", "198.51.100.2")));
        assertThrows(IllegalArgumentException.class, () -> new IpDelCliente(1, ""));
    }

    @Test
    void exigeConfianzaEnCadaProxyDeLaCadena() {
        IpDelCliente ips = new IpDelCliente(2, "192.0.2.10,192.0.2.11");
        assertEquals("198.51.100.2", ips.obtener(request("192.0.2.10", "198.51.100.2, 192.0.2.11")));
        assertEquals("192.0.2.10", ips.obtener(request("192.0.2.10", "198.51.100.2, 192.0.2.12")));
        assertEquals("192.0.2.10", ips.obtener(request("192.0.2.10", "198.51.100.2")));
    }

    @Test
    void aceptaRangosCidrIpv4YExactos() {
        IpDelCliente ips = new IpDelCliente(1, "10.0.0.0/8,192.0.2.10");
        assertEquals("198.51.100.2", ips.obtener(request("10.20.30.40", "198.51.100.2")));
        assertEquals("198.51.100.3", ips.obtener(request("192.0.2.10", "198.51.100.3")));
        assertEquals("192.0.2.11", ips.obtener(request("192.0.2.11", "198.51.100.2")));
        assertEquals("11.0.0.1", ips.obtener(request("11.0.0.1", "198.51.100.2")));
    }

    @Test
    void aceptaRangosCidrIpv6YNoMezclaFamilias() {
        IpDelCliente ips = new IpDelCliente(1, "fd00::/8,2001:db8::/32");
        assertEquals("2001:db8:1:2:3:4:5:6",
                ips.obtener(request("fd00::10", "2001:db8:1:2:3:4:5:6")));
        assertEquals("2001:db9:0:0:0:0:0:20", ips.obtener(request("2001:db9::20", "2001:db8::1")));
        assertEquals("192.0.2.10", ips.obtener(request("192.0.2.10", "2001:db8::1")));
    }

    @Test
    void laCadenaRevisaTodosLosSaltosDentroDelRango() {
        IpDelCliente ips = new IpDelCliente(2, "10.0.0.0/8");
        assertEquals("203.0.113.9", ips.obtener(request("10.1.2.3", "203.0.113.9, 10.9.9.9")));
        assertEquals("10.1.2.3", ips.obtener(request("10.1.2.3", "203.0.113.9, 192.0.2.5")));
    }

    @Test
    void rechazaPrefijosInvalidosAlArrancar() {
        for (String rango : new String[] { "10.0.0.0/33", "fd00::/129", "10.0.0.0/abc",
                "10.0.0.0/", "10.0.0.0/8/8", "10.0.0.0/-1" }) {
            assertThrows(IllegalArgumentException.class, () -> new IpDelCliente(1, rango), rango);
        }
    }

    @Test
    void recuperaPeticionOriginalAunqueSpringLaEnvuelva() throws Exception {
        MockHttpServletRequest request = request("192.0.2.10", "203.0.113.1, 198.51.100.2");
        new ForwardedHeaderFilter().doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            var envuelta = new HttpServletRequestWrapper((jakarta.servlet.http.HttpServletRequest) req);
            assertEquals("198.51.100.2", new IpDelCliente(1, "192.0.2.10").obtener(envuelta));
            assertEquals("192.0.2.10", new IpDelCliente(0, "").obtener(envuelta));
        });
    }

    @Test
    void direccionRemotaNoLiteralNoRompeLaPeticion() {
        assertEquals("desconocida", new IpDelCliente(0, "").obtener(request("fe80::1%eth0", null)));
    }

    @Test
    void elRangoSeComparaPorContenidoYNoPorReferencia() throws Exception {
        // Rango es privado: se construye por reflexion con su fabrica Rango.de(...).
        Class<?> tipo = Class.forName(IpDelCliente.class.getName() + "$Rango");
        Method de = tipo.getDeclaredMethod("de", String.class);
        de.setAccessible(true);
        Object a = de.invoke(null, "10.0.0.0/8");
        Object b = de.invoke(null, "10.0.0.0/8");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals("Rango[red=[10, 0, 0, 0], prefijo=8]", a.toString());
        assertNotEquals(a, de.invoke(null, "10.0.0.0/16"));
        assertNotEquals(a, de.invoke(null, "11.0.0.0/8"));
        assertNotEquals(a, "10.0.0.0/8");
    }

    /** Captura los avisos de IpDelCliente mientras corre {@code accion}. */
    private List<String> avisos(Runnable accion) {
        Logger logger = (Logger) LoggerFactory.getLogger(IpDelCliente.class);
        ListAppender<ILoggingEvent> capturados = new ListAppender<>();
        capturados.start();
        logger.addAppender(capturados);
        try {
            accion.run();
        } finally {
            logger.detachAppender(capturados);
        }
        return capturados.list.stream().filter(e -> e.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    void avisaUnaSolaVezCuandoUnSaltoNoEsProxyConfiable() {
        IpDelCliente ips = new IpDelCliente(2, "10.0.0.0/8");
        List<String> avisos = avisos(() -> {
            assertEquals("10.1.2.3", ips.obtener(request("10.1.2.3", "203.0.113.9, 104.22.17.40")));
            assertEquals("10.1.2.3", ips.obtener(request("10.1.2.3", "198.51.100.7, 104.22.17.41")));
        });
        assertEquals(1, avisos.size());
        assertTrue(avisos.get(0).contains("(10.1.2.3)"), avisos.get(0));
        assertTrue(avisos.get(0).contains("la entrada 2 de X-Forwarded-For no es un proxy confiable"), avisos.get(0));
        assertTrue(avisos.get(0).contains("[203.0.113.9, 104.22.17.40]"), avisos.get(0));
    }

    @Test
    void avisaCuandoLaConexionNoVieneDeUnProxyConfiable() {
        IpDelCliente ips = new IpDelCliente(2, "10.0.0.0/8");
        List<String> avisos = avisos(() ->
                assertEquals("172.16.0.5", ips.obtener(request("172.16.0.5", "203.0.113.9, 10.9.9.9"))));
        assertEquals(1, avisos.size());
        assertTrue(avisos.get(0).contains("la conexión no viene de un proxy confiable"), avisos.get(0));
    }

    @Test
    void avisaCuandoFaltanEntradasEnLaCadena() {
        IpDelCliente ips = new IpDelCliente(2, "10.0.0.0/8");
        List<String> avisos = avisos(() -> assertEquals("10.1.2.3", ips.obtener(request("10.1.2.3", "203.0.113.9"))));
        assertTrue(avisos.get(0).contains("trae 1 entradas y se esperan al menos 2"), avisos.get(0));
    }

    @Test
    void elAvisoNoCopiaSaltosDeLineaDeLaCabecera() {
        IpDelCliente ips = new IpDelCliente(1, "10.0.0.0/8");
        List<String> avisos = avisos(() ->
                assertEquals("10.1.2.3", ips.obtener(request("10.1.2.3", "1.2.3.4\nWARN falso"))));
        assertEquals(1, avisos.size());
        assertTrue(avisos.get(0).contains("dirección inválida"), avisos.get(0));
        assertTrue(!avisos.get(0).contains("\n") && !avisos.get(0).contains("WARN falso"), avisos.get(0));
    }

    @Test
    void noAvisaCuandoLaCadenaSeUsa() {
        IpDelCliente ips = new IpDelCliente(2, "10.0.0.0/8,104.16.0.0/13");
        List<String> avisos = avisos(() ->
                assertEquals("203.0.113.9", ips.obtener(request("10.1.2.3", "203.0.113.9, 104.22.17.40"))));
        assertTrue(avisos.isEmpty(), avisos.toString());
    }
}
