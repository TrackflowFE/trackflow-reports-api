package com.trackflow.modules.reports.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LimitesDePeriodoConfigTest {

    private final LimitesDePeriodoConfig config = new LimitesDePeriodoConfig();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void usaLosValoresPorDefecto() {
        var limites = config.limitesDePeriodo("2026-01-01", 366, clock);

        assertEquals(LocalDate.of(2026, 1, 1), limites.inicioOperacion());
        assertEquals(366, limites.maximoDias());
    }

    @Test
    void rechazaUnInicioEnElFuturo() {
        assertThrows(IllegalArgumentException.class,
                () -> config.limitesDePeriodo("2027-01-01", 366, clock));
    }

    @Test
    void rechazaUnMaximoMenorQueUnDia() {
        assertThrows(IllegalArgumentException.class,
                () -> config.limitesDePeriodo("2026-01-01", 0, clock));
    }

    @Test
    void rechazaUnaFechaMalFormada() {
        assertThrows(IllegalArgumentException.class,
                () -> config.limitesDePeriodo("01/01/2026", 366, clock));
    }
}
