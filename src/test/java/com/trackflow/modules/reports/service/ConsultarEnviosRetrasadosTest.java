package com.trackflow.modules.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackflow.modules.reports.application.ConsultarEnviosRetrasados;
import com.trackflow.modules.reports.application.EnviosEnRedRepository;
import com.trackflow.modules.reports.application.EnviosRetrasados;
import com.trackflow.modules.reports.domain.ReglaDeRetraso;
import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarEnviosRetrasadosTest {

    private static final Instant AHORA = Instant.parse("2026-10-10T17:00:00Z");

    @Mock
    private EnviosEnRedRepository repository;

    @InjectMocks
    private ConsultarEnviosRetrasados consulta;

    @Test
    @DisplayName("evalúa los envíos pendientes devueltos por el repositorio")
    void evaluaLosEnviosEnRed() {
        // Arrange
        consulta = new ConsultarEnviosRetrasados(repository, ReglaDeRetraso.porDefecto(),
                Clock.fixed(AHORA, ZoneOffset.UTC));
        var view = new ShipmentTrackingView("TF-100", "IN_TRANSIT",
                new ShipmentTrackingView.Origen("Ana", 1L, "Medellín"),
                new ShipmentTrackingView.Destino("Luis", "Calle 1", 2L, "Bogotá"),
                AHORA.minus(Duration.ofHours(72)));
        when(repository.buscarEnRed()).thenReturn(List.of(view));

        // Act
        EnviosRetrasados resultado = consulta.ejecutar();

        // Assert
        assertThat(resultado.corte()).isEqualTo(AHORA);
        assertThat(resultado.hayDatos()).isTrue();
        assertThat(resultado.envios()).singleElement().satisfies(retraso -> {
            assertThat(retraso.trackingNumber()).isEqualTo("TF-100");
            assertThat(retraso.detenido()).isEqualTo(Duration.ofHours(72));
            assertThat(retraso.exceso()).isEqualTo(Duration.ofHours(24));
        });
        verify(repository).buscarEnRed();
    }
}
