package com.trackflow.modules.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import com.trackflow.modules.reports.infrastructure.JpaShipmentTrackingViewRepository;
import com.trackflow.modules.reports.infrastructure.ShipmentTrackingViewJpaRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaShipmentTrackingViewRepositoryTest {

    @Mock
    private ShipmentTrackingViewJpaRepository jpa;

    @Test
    @DisplayName("guarda una vista de seguimiento")
    void guardaVista() {
        // Arrange
        var repository = new JpaShipmentTrackingViewRepository(jpa);
        var view = crearVista("TF-1");
        when(jpa.save(view)).thenReturn(view);

        // Act
        ShipmentTrackingView resultado = repository.save(view);

        // Assert
        assertThat(resultado).isSameAs(view);
        verify(jpa).save(view);
    }

    @Test
    @DisplayName("propaga el error del almacenamiento al guardar una vista")
    void propagaErrorAlGuardarVista() {
        // Arrange
        var repository = new JpaShipmentTrackingViewRepository(jpa);
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
        var repository = new JpaShipmentTrackingViewRepository(jpa);
        var view = crearVista("TF-1");
        when(jpa.findById("TF-1")).thenReturn(Optional.of(view));

        // Act
        Optional<ShipmentTrackingView> resultado = repository.findByTrackingNumber("TF-1");

        // Assert
        assertThat(resultado).containsSame(view);
        verify(jpa).findById("TF-1");
    }

    @Test
    @DisplayName("devuelve vacío cuando no encuentra el número de seguimiento")
    void devuelveVacioSiNoEncuentraVista() {
        // Arrange
        var repository = new JpaShipmentTrackingViewRepository(jpa);
        when(jpa.findById("TF-404")).thenReturn(Optional.empty());

        // Act
        Optional<ShipmentTrackingView> resultado = repository.findByTrackingNumber("TF-404");

        // Assert
        assertThat(resultado).isEmpty();
        verify(jpa).findById("TF-404");
    }

    @Test
    @DisplayName("indica que una vista guardada existe")
    void indicaQueExisteLaVista() {
        // Arrange
        var repository = new JpaShipmentTrackingViewRepository(jpa);
        when(jpa.existsById("TF-1")).thenReturn(true);

        // Act
        boolean resultado = repository.exists("TF-1");

        // Assert
        assertThat(resultado).isTrue();
        verify(jpa).existsById("TF-1");
    }

    @Test
    @DisplayName("indica que una vista inexistente no existe")
    void indicaQueNoExisteLaVista() {
        // Arrange
        var repository = new JpaShipmentTrackingViewRepository(jpa);
        when(jpa.existsById("TF-404")).thenReturn(false);

        // Act
        boolean resultado = repository.exists("TF-404");

        // Assert
        assertThat(resultado).isFalse();
        verify(jpa).existsById("TF-404");
    }

    private static ShipmentTrackingView crearVista(String tracking) {
        return new ShipmentTrackingView(tracking, "IN_TRANSIT",
                new ShipmentTrackingView.Origen("Ana", 1L, "Medellín"),
                new ShipmentTrackingView.Destino("Luis", "Calle 1", 2L, "Bogotá"),
                Instant.parse("2026-10-01T12:00:00Z"));
    }
}
