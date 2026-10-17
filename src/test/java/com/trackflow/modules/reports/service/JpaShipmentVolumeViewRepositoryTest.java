package com.trackflow.modules.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackflow.modules.reports.application.VolumenPorPunto;
import com.trackflow.modules.reports.domain.ShipmentVolumeView;
import com.trackflow.modules.reports.infrastructure.JpaShipmentVolumeViewRepository;
import com.trackflow.modules.reports.infrastructure.ShipmentVolumeViewJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaShipmentVolumeViewRepositoryTest {

    @Mock
    private ShipmentVolumeViewJpaRepository jpa;

    @Test
    @DisplayName("guarda una vista de volumen")
    void guardaVista() {
        // Arrange
        var repository = new JpaShipmentVolumeViewRepository(jpa);
        var view = crearVista("TF-1");
        when(jpa.save(view)).thenReturn(view);

        // Act
        ShipmentVolumeView resultado = repository.save(view);

        // Assert
        assertThat(resultado).isSameAs(view);
        verify(jpa).save(view);
    }

    @Test
    @DisplayName("propaga el error del almacenamiento al guardar una vista")
    void propagaErrorAlGuardarVista() {
        // Arrange
        var repository = new JpaShipmentVolumeViewRepository(jpa);
        var view = crearVista("TF-1");
        var error = new IllegalArgumentException("vista inválida");
        when(jpa.save(view)).thenThrow(error);

        // Act / Assert
        assertThatThrownBy(() -> repository.save(view)).isSameAs(error);
    }

    @Test
    @DisplayName("encuentra la vista por su número de seguimiento")
    void encuentraVista() {
        // Arrange
        var repository = new JpaShipmentVolumeViewRepository(jpa);
        var view = crearVista("TF-1");
        when(jpa.findById("TF-1")).thenReturn(Optional.of(view));

        // Act
        Optional<ShipmentVolumeView> resultado = repository.findByTrackingNumber("TF-1");

        // Assert
        assertThat(resultado).containsSame(view);
        verify(jpa).findById("TF-1");
    }

    @Test
    @DisplayName("devuelve vacío cuando no encuentra la vista")
    void devuelveVacioSiNoEncuentraVista() {
        // Arrange
        var repository = new JpaShipmentVolumeViewRepository(jpa);
        when(jpa.findById("TF-404")).thenReturn(Optional.empty());

        // Act
        Optional<ShipmentVolumeView> resultado = repository.findByTrackingNumber("TF-404");

        // Assert
        assertThat(resultado).isEmpty();
        verify(jpa).findById("TF-404");
    }

    @Test
    @DisplayName("mapea los envíos recibidos agrupados por punto de ingreso")
    void mapeaEnviosPorPuntoDeIngreso() {
        // Arrange
        var repository = new JpaShipmentVolumeViewRepository(jpa);
        var desde = Instant.parse("2026-10-01T00:00:00Z");
        var hasta = Instant.parse("2026-11-01T00:00:00Z");
        var row = mock(ShipmentVolumeViewJpaRepository.PuntoAgrupado.class);
        when(row.getCentroId()).thenReturn(10L);
        when(row.getPunto()).thenReturn("Centro Medellín");
        when(row.getCiudad()).thenReturn("Medellín");
        when(row.getEnvios()).thenReturn(4L);
        when(jpa.contarPorPuntoDeIngreso(desde, hasta)).thenReturn(List.of(row));

        // Act
        List<VolumenPorPunto> resultado = repository.registradosPorPuntoDeIngreso(desde, hasta);

        // Assert
        assertThat(resultado).containsExactly(
                new VolumenPorPunto(10L, "Centro Medellín", "Medellín", false, 4));
        verify(jpa).contarPorPuntoDeIngreso(desde, hasta);
    }

    @Test
    @DisplayName("devuelve una lista vacía si no hay envíos recibidos en el rango")
    void devuelveListaVaciaSinIngresos() {
        // Arrange
        var repository = new JpaShipmentVolumeViewRepository(jpa);
        var desde = Instant.parse("2026-10-01T00:00:00Z");
        var hasta = Instant.parse("2026-11-01T00:00:00Z");
        when(jpa.contarPorPuntoDeIngreso(desde, hasta)).thenReturn(List.of());

        // Act
        List<VolumenPorPunto> resultado = repository.registradosPorPuntoDeIngreso(desde, hasta);

        // Assert
        assertThat(resultado).isEmpty();
        verify(jpa).contarPorPuntoDeIngreso(desde, hasta);
    }

    @Test
    @DisplayName("propaga el error al agrupar envíos por punto de ingreso")
    void propagaErrorAlAgruparPorPunto() {
        // Arrange
        var repository = new JpaShipmentVolumeViewRepository(jpa);
        var desde = Instant.parse("2026-10-01T00:00:00Z");
        var hasta = Instant.parse("2026-11-01T00:00:00Z");
        var error = new IllegalStateException("consulta fallida");
        when(jpa.contarPorPuntoDeIngreso(desde, hasta)).thenThrow(error);

        // Act / Assert
        assertThatThrownBy(() -> repository.registradosPorPuntoDeIngreso(desde, hasta)).isSameAs(error);
    }

    @Test
    @DisplayName("mapea los envíos pendientes agrupados por ciudad de origen")
    void mapeaEnviosPendientesPorCiudad() {
        // Arrange
        var repository = new JpaShipmentVolumeViewRepository(jpa);
        var desde = Instant.parse("2026-10-01T00:00:00Z");
        var hasta = Instant.parse("2026-11-01T00:00:00Z");
        var row = mock(ShipmentVolumeViewJpaRepository.CiudadAgrupada.class);
        when(row.getCiudad()).thenReturn("Cali");
        when(row.getEnvios()).thenReturn(2L);
        when(jpa.contarPendientesPorCiudadDeOrigen(desde, hasta)).thenReturn(List.of(row));

        // Act
        List<VolumenPorPunto> resultado = repository.registradosPendientesDeRecepcion(desde, hasta);

        // Assert
        assertThat(resultado).containsExactly(
                new VolumenPorPunto(null, "Pendiente de recepción en Cali", "Cali", true, 2));
        verify(jpa).contarPendientesPorCiudadDeOrigen(desde, hasta);
    }

    @Test
    @DisplayName("devuelve una lista vacía si no hay envíos pendientes de recepción")
    void devuelveListaVaciaSinPendientes() {
        // Arrange
        var repository = new JpaShipmentVolumeViewRepository(jpa);
        var desde = Instant.parse("2026-10-01T00:00:00Z");
        var hasta = Instant.parse("2026-11-01T00:00:00Z");
        when(jpa.contarPendientesPorCiudadDeOrigen(desde, hasta)).thenReturn(List.of());

        // Act
        List<VolumenPorPunto> resultado = repository.registradosPendientesDeRecepcion(desde, hasta);

        // Assert
        assertThat(resultado).isEmpty();
        verify(jpa).contarPendientesPorCiudadDeOrigen(desde, hasta);
    }

    @Test
    @DisplayName("propaga el error al agrupar envíos pendientes por ciudad")
    void propagaErrorAlAgruparPendientesPorCiudad() {
        // Arrange
        var repository = new JpaShipmentVolumeViewRepository(jpa);
        var desde = Instant.parse("2026-10-01T00:00:00Z");
        var hasta = Instant.parse("2026-11-01T00:00:00Z");
        var error = new IllegalStateException("consulta fallida");
        when(jpa.contarPendientesPorCiudadDeOrigen(desde, hasta)).thenThrow(error);

        // Act / Assert
        assertThatThrownBy(() -> repository.registradosPendientesDeRecepcion(desde, hasta)).isSameAs(error);
    }

    @Test
    @DisplayName("cuenta los envíos entregados en el rango solicitado")
    void cuentaEntregasEnElRango() {
        // Arrange
        var repository = new JpaShipmentVolumeViewRepository(jpa);
        var desde = Instant.parse("2026-10-01T00:00:00Z");
        var hasta = Instant.parse("2026-11-01T00:00:00Z");
        when(jpa.contarEntregados(desde, hasta)).thenReturn(5L);

        // Act
        long resultado = repository.entregadosEntre(desde, hasta);

        // Assert
        assertThat(resultado).isEqualTo(5L);
        verify(jpa).contarEntregados(desde, hasta);
    }

    @Test
    @DisplayName("devuelve cero entregas cuando no hay resultados en el rango")
    void devuelveCeroEntregasSinResultados() {
        // Arrange
        var repository = new JpaShipmentVolumeViewRepository(jpa);
        var desde = Instant.parse("2026-10-01T00:00:00Z");
        var hasta = Instant.parse("2026-11-01T00:00:00Z");
        when(jpa.contarEntregados(desde, hasta)).thenReturn(0L);

        // Act
        long resultado = repository.entregadosEntre(desde, hasta);

        // Assert
        assertThat(resultado).isZero();
        verify(jpa).contarEntregados(desde, hasta);
    }

    private static ShipmentVolumeView crearVista(String tracking) {
        return new ShipmentVolumeView(tracking, Instant.parse("2026-10-01T12:00:00Z"),
                1L, "Medellín");
    }
}
