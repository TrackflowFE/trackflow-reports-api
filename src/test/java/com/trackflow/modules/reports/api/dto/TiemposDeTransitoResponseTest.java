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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TiemposDeTransitoResponseTest {

    @ParameterizedTest
    @CsvSource({"0,menos de 1 min", "1,menos de 1 min", "59,menos de 1 min", "60,1 min",
            "3599,59 min", "3600,1 h", "86399,23 h 59 min", "86400,1 d",
            "86460,1 d 1 min", "90000,1 d 1 h", "90060,1 d 1 h 1 min",
            "-1,menos de 1 min", "-60,menos de 1 min", "-3600,menos de 1 min", "-86400,menos de 1 min"})
    void limitesYNegativosConservanTextoYSegundos(long seconds, String text) {
        var duration = Duration.ofSeconds(seconds);
        assertEquals(text, TiemposDeTransitoResponse.formatear(duration));
        assertEquals(new TiemposDeTransitoResponse.Promedio(seconds, text),
                TiemposDeTransitoResponse.Promedio.of(duration));
    }

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
