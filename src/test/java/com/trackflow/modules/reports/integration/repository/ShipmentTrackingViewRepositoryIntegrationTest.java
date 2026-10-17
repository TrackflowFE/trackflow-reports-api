package com.trackflow.modules.reports.integration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.trackflow.modules.reports.application.ShipmentTrackingViewRepository;
import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShipmentTrackingViewRepositoryIntegrationTest {

    @Autowired
    private ShipmentTrackingViewRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void guardaYConsultaElSeguimientoPersistido() {
        // Arrange
        ShipmentTrackingView envio = new ShipmentTrackingView(
                "TRACK-1",
                "IN_TRANSIT",
                new ShipmentTrackingView.Origen("Remitente", 10L, "Bogota"),
                new ShipmentTrackingView.Destino("Destinatario", "Calle 1", 20L, "Medellin"),
                Instant.parse("2026-10-01T10:00:00Z"));
        envio.registrarMovimiento("IN_TRANSIT", "Centro Norte", 10L, "Bogota",
                Instant.parse("2026-10-02T10:00:00Z"));

        // Act
        repository.save(envio);
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(repository.exists("TRACK-1")).isTrue();
        ShipmentTrackingView recuperado = repository.findByTrackingNumber("TRACK-1").orElseThrow();
        assertThat(recuperado.getStatus()).isEqualTo("IN_TRANSIT");
        assertThat(recuperado.getSenderName()).isEqualTo("Remitente");
        assertThat(recuperado.getDestinationCity()).isEqualTo("Medellin");
        assertThat(recuperado.getLastMovementPoint()).isEqualTo("Centro Norte");
        assertThat(recuperado.getLastMovementAt()).isEqualTo(Instant.parse("2026-10-02T10:00:00Z"));
    }

    @Test
    void devuelveAusenciaYExistsFalseCuandoElSeguimientoNoEstaGuardado() {
        // Arrange
        String trackingNumber = "TRACK-INEXISTENTE";

        // Act / Assert
        assertThat(repository.findByTrackingNumber(trackingNumber)).isEmpty();
        assertThat(repository.exists(trackingNumber)).isFalse();
    }
}
