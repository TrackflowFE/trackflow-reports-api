package com.trackflow.modules.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackflow.modules.reports.application.ShipmentTransitViewRepository.Medicion;
import com.trackflow.modules.reports.domain.ShipmentTransitView;
import com.trackflow.modules.reports.infrastructure.JpaShipmentTransitViewRepository;
import com.trackflow.modules.reports.infrastructure.ShipmentTransitViewJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaShipmentTransitViewRepositoryTest {

    @Mock
    private ShipmentTransitViewJpaRepository jpa;

    @Test
    @DisplayName("guarda una vista de tiempos de tránsito")
    void guardaVista() {
        // Arrange
        var repository = new JpaShipmentTransitViewRepository(jpa);
        var view = new ShipmentTransitView("TF-1");
        when(jpa.save(view)).thenReturn(view);

        // Act
        ShipmentTransitView resultado = repository.save(view);

        // Assert
        assertThat(resultado).isSameAs(view);
        verify(jpa).save(view);
    }

    @Test
    @DisplayName("propaga el error del almacenamiento al guardar una vista")
    void propagaErrorAlGuardarVista() {
        // Arrange
        var repository = new JpaShipmentTransitViewRepository(jpa);
        var view = new ShipmentTransitView("TF-1");
        var error = new IllegalArgumentException("vista inválida");
        when(jpa.save(view)).thenThrow(error);

        // Act / Assert
        assertThatThrownBy(() -> repository.save(view)).isSameAs(error);
    }

    @Test
    @DisplayName("encuentra la vista por número de seguimiento")
    void encuentraVista() {
        // Arrange
        var repository = new JpaShipmentTransitViewRepository(jpa);
        var view = new ShipmentTransitView("TF-1");
        when(jpa.findById("TF-1")).thenReturn(Optional.of(view));

        // Act
        Optional<ShipmentTransitView> resultado = repository.findByTrackingNumber("TF-1");

        // Assert
        assertThat(resultado).containsSame(view);
        verify(jpa).findById("TF-1");
    }

    @Test
    @DisplayName("devuelve vacío cuando no encuentra la vista")
    void devuelveVacioSiNoEncuentraVista() {
        // Arrange
        var repository = new JpaShipmentTransitViewRepository(jpa);
        when(jpa.findById("TF-404")).thenReturn(Optional.empty());

        // Act
        Optional<ShipmentTransitView> resultado = repository.findByTrackingNumber("TF-404");

        // Assert
        assertThat(resultado).isEmpty();
        verify(jpa).findById("TF-404");
    }

    @Test
    @DisplayName("cuenta las entregas en el rango solicitado")
    void cuentaEntregasEnElRango() {
        // Arrange
        var repository = new JpaShipmentTransitViewRepository(jpa);
        var desde = Instant.parse("2026-10-01T00:00:00Z");
        var hasta = Instant.parse("2026-11-01T00:00:00Z");
        when(jpa.contarEntregados(desde, hasta)).thenReturn(7L);

        // Act
        long resultado = repository.contarEntregadosEntre(desde, hasta);

        // Assert
        assertThat(resultado).isEqualTo(7L);
        verify(jpa).contarEntregados(desde, hasta);
    }

    @Test
    @DisplayName("devuelve cero entregas cuando el rango no contiene resultados")
    void devuelveCeroEntregasSinResultados() {
        // Arrange
        var repository = new JpaShipmentTransitViewRepository(jpa);
        var desde = Instant.parse("2026-10-01T00:00:00Z");
        var hasta = Instant.parse("2026-11-01T00:00:00Z");
        when(jpa.contarEntregados(desde, hasta)).thenReturn(0L);

        // Act
        long resultado = repository.contarEntregadosEntre(desde, hasta);

        // Assert
        assertThat(resultado).isZero();
        verify(jpa).contarEntregados(desde, hasta);
    }

    @Test
    @DisplayName("convierte los promedios consultados a mediciones por tramo")
    void conviertePromediosEnMediciones() {
        // Arrange
        var repository = new JpaShipmentTransitViewRepository(jpa);
        var desde = Instant.parse("2026-10-01T00:00:00Z");
        var hasta = Instant.parse("2026-11-01T00:00:00Z");
        var row = mock(ShipmentTransitViewJpaRepository.TramoPromedio.class);
        when(row.getTramo()).thenReturn("TRANSPORT");
        when(row.getPromedio()).thenReturn(3600.25);
        when(row.getEnvios()).thenReturn(3L);
        when(jpa.promediarEntregados(desde, hasta)).thenReturn(List.of(row));

        // Act
        var resultado = repository.medirEntregadosEntre(desde, hasta);

        // Assert
        assertThat(resultado).containsEntry("TRANSPORT", new Medicion(Duration.ofMillis(3_600_250), 3));
        verify(jpa).promediarEntregados(desde, hasta);
    }

    @Test
    @DisplayName("devuelve un mapa vacío cuando no hay promedios por tramo")
    void devuelveMapaVacioSinPromedios() {
        // Arrange
        var repository = new JpaShipmentTransitViewRepository(jpa);
        var desde = Instant.parse("2026-10-01T00:00:00Z");
        var hasta = Instant.parse("2026-11-01T00:00:00Z");
        when(jpa.promediarEntregados(desde, hasta)).thenReturn(List.of());

        // Act
        var resultado = repository.medirEntregadosEntre(desde, hasta);

        // Assert
        assertThat(resultado).isEmpty();
        verify(jpa).promediarEntregados(desde, hasta);
    }

    @Test
    @DisplayName("propaga el error del almacenamiento al consultar promedios")
    void propagaErrorAlConsultarPromedios() {
        // Arrange
        var repository = new JpaShipmentTransitViewRepository(jpa);
        var desde = Instant.parse("2026-10-01T00:00:00Z");
        var hasta = Instant.parse("2026-11-01T00:00:00Z");
        var error = new IllegalStateException("consulta fallida");
        when(jpa.promediarEntregados(desde, hasta)).thenThrow(error);

        // Act / Assert
        assertThatThrownBy(() -> repository.medirEntregadosEntre(desde, hasta)).isSameAs(error);
    }
}
