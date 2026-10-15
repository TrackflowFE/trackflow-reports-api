package com.trackflow.modules.reports.api.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trackflow.modules.reports.application.TiemposDeTransito;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TiemposDeTransitoResponseTest {

    @Test
    void formateaLaDuracionOmitiendoUnidadesEnCero() {
        assertEquals("1 d 4 h 30 min", TiemposDeTransitoResponse.formatear(Duration.ofMinutes(24 * 60 + 4 * 60 + 30)));
        assertEquals("3 h", TiemposDeTransitoResponse.formatear(Duration.ofHours(3)));
        assertEquals("menos de 1 min", TiemposDeTransitoResponse.formatear(Duration.ofSeconds(20)));
    }

    @Test
    void sinEntregasElMensajeInformaQueNoHayDatos() {
        LocalDate dia = LocalDate.of(2026, 10, 5);
        TiemposDeTransitoResponse r = TiemposDeTransitoResponse.from(
                new TiemposDeTransito(dia, dia, Instant.EPOCH, false, 0, null, List.of()));

        assertEquals(0, r.deliveredShipments());
        assertNull(r.averageTotal());
        assertTrue(r.segments().isEmpty());
        assertTrue(r.message().startsWith("No hay datos"));
    }
}
