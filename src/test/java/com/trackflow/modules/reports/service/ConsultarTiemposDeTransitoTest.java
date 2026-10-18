package com.trackflow.modules.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.trackflow.modules.reports.application.ConsultarTiemposDeTransito;
import com.trackflow.modules.reports.application.ShipmentTransitViewRepository;
import com.trackflow.modules.reports.application.ShipmentTransitViewRepository.Medicion;
import com.trackflow.modules.reports.application.TiemposDeTransito;
import com.trackflow.modules.reports.application.TramoDeTransito;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarTiemposDeTransitoTest {

    private static final Instant AHORA = Instant.parse("2026-10-10T17:00:00Z");
    private static final Instant INICIO = Instant.parse("2026-10-10T05:00:00Z");

    @Mock
    private ShipmentTransitViewRepository repository;

    private ConsultarTiemposDeTransito consulta;

    @Test
    @DisplayName("devuelve un reporte vacío sin calcular promedios si no hay entregas")
    void devuelveUnReporteVacioSinEntregas() {
        // Arrange
        consulta = new ConsultarTiemposDeTransito(repository, Clock.fixed(AHORA, ZoneOffset.UTC));
        when(repository.contarEntregadosEntre(INICIO, AHORA)).thenReturn(0L);

        // Act
        TiemposDeTransito resultado = consulta.ejecutar(
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 10));

        // Assert
        assertThat(resultado.hayDatos()).isFalse();
        assertThat(resultado.promedioTotal()).isNull();
        assertThat(resultado.tramos()).isEmpty();
        verify(repository).contarEntregadosEntre(INICIO, AHORA);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("mapea las mediciones totales y por tramo")
    void devuelveLosPromediosDeLosTramos() {
        // Arrange
        consulta = new ConsultarTiemposDeTransito(repository, Clock.fixed(AHORA, ZoneOffset.UTC));
        when(repository.contarEntregadosEntre(INICIO, AHORA)).thenReturn(2L);
        when(repository.medirEntregadosEntre(INICIO, AHORA)).thenReturn(Map.of(
                ShipmentTransitViewRepository.TOTAL, new Medicion(Duration.ofHours(48), 2),
                TramoDeTransito.TRANSPORTE.codigo(), new Medicion(Duration.ofHours(12), 2)));

        // Act
        TiemposDeTransito resultado = consulta.ejecutar(
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 10));

        // Assert
        assertThat(resultado.enviosEntregados()).isEqualTo(2);
        assertThat(resultado.promedioTotal()).isEqualTo(Duration.ofHours(48));
        assertThat(resultado.tramos())
                .filteredOn(tramo -> tramo.tramo() == TramoDeTransito.TRANSPORTE)
                .singleElement()
                .satisfies(tramo -> {
                    assertThat(tramo.promedio()).isEqualTo(Duration.ofHours(12));
                    assertThat(tramo.envios()).isEqualTo(2);
                });
        assertThat(resultado.tramos())
                .filteredOn(tramo -> tramo.tramo() == TramoDeTransito.REPARTO)
                .singleElement()
                .satisfies(tramo -> {
                    assertThat(tramo.promedio()).isNull();
                    assertThat(tramo.envios()).isZero();
                });
        verify(repository).contarEntregadosEntre(INICIO, AHORA);
        verify(repository).medirEntregadosEntre(INICIO, AHORA);
    }
}
