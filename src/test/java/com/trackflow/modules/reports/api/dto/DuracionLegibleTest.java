package com.trackflow.modules.reports.api.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DuracionLegibleTest {

    @ParameterizedTest
    @CsvSource({"2068,86 d 4 h", "48,2 d", "24,1 d", "23,23 h", "1,1 h", "0,menos de 1 h"})
    void diasYHorasMuestraDiasYLaParteMenorEnHoras(long horas, String texto) {
        assertEquals(texto, DuracionLegible.diasYHoras(Duration.ofHours(horas)));
    }

    @Test
    void noventaMinutosSonUnaHoraYTreintaMenosDeUna() {
        assertEquals("1 h", DuracionLegible.diasYHoras(Duration.ofMinutes(90)));
        assertEquals("menos de 1 h", DuracionLegible.diasYHoras(Duration.ofMinutes(30)));
    }

    @ParameterizedTest
    @CsvSource({"0,menos de 1 min", "60,1 min", "3600,1 h", "86400,1 d",
            "90000,1 d 1 h", "90060,1 d 1 h 1 min", "-60,menos de 1 min"})
    void conMinutosConservaElFormatoDeLosPromedios(long segundos, String texto) {
        assertEquals(texto, DuracionLegible.conMinutos(Duration.ofSeconds(segundos)));
    }
}
