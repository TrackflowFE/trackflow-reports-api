package com.trackflow.modules.reports.api.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.trackflow.modules.reports.application.EnviosRetrasados;
import com.trackflow.modules.reports.domain.Severidad;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EnviosRetrasadosResponseTest {
    private static final Map<Severidad, String> TEXTS = Map.of(
            Severidad.LEVE, "LOW", Severidad.MODERADA, "MODERATE", Severidad.CRITICA, "CRITICAL");

    @ParameterizedTest
    @EnumSource(Severidad.class)
    void todasLasSeveridadesConservanTextoHttpYHorasTruncadas(Severidad severity) {
        assertEquals(Set.of(Severidad.values()), TEXTS.keySet());
        var result = EnviosRetrasadosResponse.from(report(severity)).shipments().getFirst();
        assertEquals(TEXTS.get(severity), result.severity());
        assertEquals(5, result.hoursStopped());
        assertEquals(2, result.hoursExpected());
        assertEquals(3, result.hoursOver());
    }

    @Test
    void severidadNulaSigueFallando() {
        assertThrows(NullPointerException.class, () -> EnviosRetrasadosResponse.from(report(null)));
    }

    private EnviosRetrasados report(Severidad severity) {
        return new EnviosRetrasados(Instant.EPOCH, List.of(new EnviosRetrasados.Retraso(
                "TF-TEST", "IN_TRANSIT", "Center", "Origin", "Destination",
                Duration.ofMinutes(359), Duration.ofMinutes(179), severity, null, null)));
    }
}
