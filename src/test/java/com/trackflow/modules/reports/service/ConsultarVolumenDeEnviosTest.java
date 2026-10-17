package com.trackflow.modules.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trackflow.modules.reports.application.ConsultarVolumenDeEnvios;
import com.trackflow.modules.reports.application.ShipmentVolumeViewRepository;
import com.trackflow.modules.reports.application.VolumenDeEnvios;
import com.trackflow.modules.reports.application.VolumenPorPunto;
import com.trackflow.modules.reports.domain.PeriodoInvalidoException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarVolumenDeEnviosTest {

    private static final Instant AHORA = Instant.parse("2026-10-10T17:00:00Z");

    @Mock
    private ShipmentVolumeViewRepository repository;

    private ConsultarVolumenDeEnvios consulta;

    @Test
    @DisplayName("agrupa, ordena y suma los envíos del periodo")
    void devuelveElVolumenOrdenadoDelPeriodo() {
        // Arrange
        var clock = Clock.fixed(AHORA, ZoneOffset.UTC);
        consulta = new ConsultarVolumenDeEnvios(repository, clock);
        var desde = LocalDate.of(2026, 10, 10);
        var hasta = desde;
        var inicio = Instant.parse("2026-10-10T05:00:00Z");
        var recibidos = List.of(
                new VolumenPorPunto(10L, "Centro Medellín", "Medellín", false, 3),
                new VolumenPorPunto(20L, "Centro Bogotá", "Bogotá", false, 5));
        var pendientes = List.of(new VolumenPorPunto(null, "Cali", "Cali", true, 2));
        when(repository.registradosPorPuntoDeIngreso(inicio, AHORA)).thenReturn(recibidos);
        when(repository.registradosPendientesDeRecepcion(inicio, AHORA)).thenReturn(pendientes);
        when(repository.entregadosEntre(inicio, AHORA)).thenReturn(4L);

        // Act
        VolumenDeEnvios resultado = consulta.ejecutar(desde, hasta);

        // Assert
        assertThat(resultado.totalRegistrados()).isEqualTo(10);
        assertThat(resultado.totalEntregados()).isEqualTo(4);
        assertThat(resultado.enCurso()).isTrue();
        assertThat(resultado.porPunto()).containsExactly(
                recibidos.get(1), recibidos.get(0), pendientes.get(0));
        verify(repository).registradosPorPuntoDeIngreso(inicio, AHORA);
        verify(repository).registradosPendientesDeRecepcion(inicio, AHORA);
        verify(repository).entregadosEntre(inicio, AHORA);
    }

    @Test
    @DisplayName("rechaza un periodo invertido antes de consultar el repositorio")
    void rechazaUnPeriodoInvertido() {
        // Arrange
        var clock = Clock.fixed(AHORA, ZoneOffset.UTC);
        consulta = new ConsultarVolumenDeEnvios(repository, clock);

        // Act / Assert
        assertThatThrownBy(() -> consulta.ejecutar(
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 9)))
                .isInstanceOf(PeriodoInvalidoException.class);
        verifyNoInteractions(repository);
    }
}
