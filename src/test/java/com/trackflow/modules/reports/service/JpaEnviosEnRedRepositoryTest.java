package com.trackflow.modules.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import com.trackflow.modules.reports.infrastructure.EnviosEnRedJpaRepository;
import com.trackflow.modules.reports.infrastructure.JpaEnviosEnRedRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaEnviosEnRedRepositoryTest {

    @Mock
    private EnviosEnRedJpaRepository jpa;

    @Test
    @DisplayName("devuelve los envíos que continúan en la red")
    void devuelveEnviosEnRed() {
        // Arrange
        var repository = new JpaEnviosEnRedRepository(jpa);
        var view = crearVista("TF-1", "IN_TRANSIT");
        when(jpa.findByStatusNot("DELIVERED")).thenReturn(List.of(view));

        // Act
        List<ShipmentTrackingView> resultado = repository.buscarEnRed();

        // Assert
        assertThat(resultado).containsExactly(view);
        verify(jpa).findByStatusNot("DELIVERED");
    }

    @Test
    @DisplayName("devuelve una lista vacía cuando no hay envíos pendientes")
    void devuelveListaVaciaCuandoNoHayEnviosEnRed() {
        // Arrange
        var repository = new JpaEnviosEnRedRepository(jpa);
        when(jpa.findByStatusNot("DELIVERED")).thenReturn(List.of());

        // Act
        List<ShipmentTrackingView> resultado = repository.buscarEnRed();

        // Assert
        assertThat(resultado).isEmpty();
        verify(jpa).findByStatusNot("DELIVERED");
    }

    @Test
    @DisplayName("propaga el error del almacenamiento al buscar envíos en red")
    void propagaErrorAlBuscarEnviosEnRed() {
        // Arrange
        var repository = new JpaEnviosEnRedRepository(jpa);
        var error = new IllegalStateException("base de datos no disponible");
        when(jpa.findByStatusNot("DELIVERED")).thenThrow(error);

        // Act / Assert
        assertThatThrownBy(repository::buscarEnRed).isSameAs(error);
    }

    private static ShipmentTrackingView crearVista(String tracking, String estado) {
        return new ShipmentTrackingView(tracking, estado,
                new ShipmentTrackingView.Origen("Ana", 1L, "Medellín"),
                new ShipmentTrackingView.Destino("Luis", "Calle 1", 2L, "Bogotá"),
                Instant.parse("2026-10-01T12:00:00Z"));
    }
}
