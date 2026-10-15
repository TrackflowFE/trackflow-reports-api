package com.trackflow.modules.reports.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trackflow.modules.reports.domain.ReglaDeRetraso;
import com.trackflow.modules.reports.domain.Severidad;
import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Escenarios de la HU de envíos retrasados, con un repositorio en memoria. */
class ReporteEnviosRetrasadosTest {

    /** Hoy es 10 de octubre de 2026, 12:00 en Colombia (17:00 UTC). */
    private static final Instant AHORA = Instant.parse("2026-10-10T17:00:00Z");
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final EnMemoria repositorio = new EnMemoria();
    private final ConsultarEnviosRetrasados consulta =
            new ConsultarEnviosRetrasados(repositorio, ReglaDeRetraso.porDefecto(), clock);

    @Test
    void losEnviosRetrasadosSeListanDelMasAtrasadoAlMenos() {
        envio("A", "IN_TRANSIT", "Autopista Medellín-Bogotá", horasAtras(72));       // exceso 24 h, de 48
        envio("B", "AT_DISTRIBUTION_CENTER", "Centro de Medellín", horasAtras(60));  // exceso 36 h, de 24
        envio("C", "OUT_FOR_DELIVERY", "Reparto Bogotá", horasAtras(14));            // exceso 2 h, de 12
        envio("D", "IN_TRANSIT", "Autopista Medellín-Bogotá", horasAtras(10));       // dentro del tiempo

        EnviosRetrasados r = consulta.ejecutar();

        assertTrue(r.hayDatos());
        assertEquals(List.of("B", "A", "C"),
                r.envios().stream().map(EnviosRetrasados.Retraso::trackingNumber).toList());
        assertEquals(List.of(Severidad.CRITICA, Severidad.MODERADA, Severidad.LEVE),
                r.envios().stream().map(EnviosRetrasados.Retraso::severidad).toList());
    }

    @Test
    void unEnvioDetenidoEnUnPuntoMuestraDondeEstaYCuantoLlevaAlli() {
        envio("A", "IN_TRANSIT", "Centro de distribución Medellín", horasAtras(72));

        EnviosRetrasados.Retraso retraso = consulta.ejecutar().envios().get(0);

        assertEquals("Centro de distribución Medellín", retraso.ultimoPunto());
        assertEquals(Duration.ofHours(72), retraso.detenido());
        assertEquals(Duration.ofHours(48), retraso.maximoEsperado());
        assertEquals(Duration.ofHours(24), retraso.exceso());
    }

    @Test
    void sinEnviosRetrasadosElReporteInformaQueNoHayNinguno() {
        envio("D", "IN_TRANSIT", "Autopista Medellín-Bogotá", horasAtras(10));
        envio("E", "DELIVERED", "Bogotá", horasAtras(200));

        EnviosRetrasados r = consulta.ejecutar();

        assertFalse(r.hayDatos());
        assertTrue(r.envios().isEmpty());
    }

    @Test
    void unEnvioRegistradoSinMovimientosSeMideDesdeSuRegistro() {
        envioSinMovimientos("F", horasAtras(30));

        EnviosRetrasados.Retraso retraso = consulta.ejecutar().envios().get(0);

        assertNull(retraso.ultimoPunto());
        assertEquals(Duration.ofHours(30), retraso.detenido());
        assertEquals(Severidad.LEVE, retraso.severidad());
    }

    @Test
    void unEnvioJustoEnElLimiteNoSeConsideraRetrasado() {
        envio("A", "IN_TRANSIT", "Autopista Medellín-Bogotá", horasAtras(48));

        assertFalse(consulta.ejecutar().hayDatos());
    }

    // --- utilidades

    private static Instant horasAtras(long horas) {
        return AHORA.minus(Duration.ofHours(horas));
    }

    private void envio(String tracking, String estado, String punto, Instant ultimoMovimiento) {
        ShipmentTrackingView vista = nueva(tracking, horasAtras(300));
        vista.registrarMovimiento(estado, punto, ultimoMovimiento);
        repositorio.vistas.add(vista);
    }

    private void envioSinMovimientos(String tracking, Instant registrado) {
        repositorio.vistas.add(nueva(tracking, registrado));
    }

    private static ShipmentTrackingView nueva(String tracking, Instant registrado) {
        return new ShipmentTrackingView(tracking, "REGISTERED",
                new ShipmentTrackingView.Origen("Ana Torres", 1L, "MEDELLÍN - ANTIOQUIA"),
                new ShipmentTrackingView.Destino("Luis Gómez", "Calle 1 # 2-3", 10L, "BOGOTÁ - CUNDINAMARCA"),
                registrado);
    }

    /** Repositorio en memoria que filtra como la consulta real: todo lo que no está entregado. */
    private static final class EnMemoria implements EnviosEnRedRepository {
        private final List<ShipmentTrackingView> vistas = new ArrayList<>();

        @Override
        public List<ShipmentTrackingView> buscarEnRed() {
            return vistas.stream().filter(v -> !"DELIVERED".equals(v.getStatus())).toList();
        }
    }
}
