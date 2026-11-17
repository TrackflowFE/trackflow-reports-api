package com.trackflow.modules.reports.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trackflow.modules.reports.domain.PeriodoInvalidoException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Los reportes no aceptan periodos anteriores a la operación ni rangos de más de un año. */
class PeriodoDeConsultaTest {

    private static final Clock HOY = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneOffset.UTC);
    private static final LimitesDePeriodo LIMITES = new LimitesDePeriodo(LocalDate.of(2026, 1, 1), 366);

    @Test
    void rechazaUnPeriodoAnteriorAlInicioDeLaOperacion() {
        PeriodoInvalidoException error = assertThrows(PeriodoInvalidoException.class,
                () -> PeriodoDeConsulta.resolver(LocalDate.of(1939, 1, 1), LocalDate.of(1944, 5, 7), HOY, LIMITES));

        assertTrue(error.getMessage().contains("2026-01-01"), error.getMessage());
        assertTrue(error.getMessage().contains("inicio de la operación"), error.getMessage());
    }

    @Test
    void aceptaElDiaExactoDeInicio() {
        PeriodoDeConsulta periodo = PeriodoDeConsulta.resolver(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), HOY, LIMITES);

        assertEquals(LocalDate.of(2026, 1, 1), periodo.desde());
        assertEquals(LocalDate.of(2026, 1, 1), periodo.hasta());
    }

    @Test
    void aceptaElMaximoDeDiasYRechazaUnoMas() {
        PeriodoDeConsulta periodo = PeriodoDeConsulta.resolver(
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), HOY, LIMITES);
        assertEquals(LocalDate.of(2027, 1, 1), periodo.hasta());

        PeriodoInvalidoException error = assertThrows(PeriodoInvalidoException.class,
                () -> PeriodoDeConsulta.resolver(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 2), HOY, LIMITES));
        assertTrue(error.getMessage().contains("367"), error.getMessage());
        assertTrue(error.getMessage().contains("366"), error.getMessage());
    }
}
