package com.trackflow.modules.reports.integration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.trackflow.modules.reports.application.EnviosEnRedRepository;
import com.trackflow.modules.reports.application.ShipmentTrackingViewRepository;
import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EnviosEnRedRepositoryIntegrationTest {

    @Autowired
    private ShipmentTrackingViewRepository trackingRepository;

    @Autowired
    private EnviosEnRedRepository enviosEnRedRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void prepararDatos() {
        trackingRepository.save(crearEnvio("TRACK-EN-RED", "IN_TRANSIT"));
        trackingRepository.save(crearEnvio("TRACK-ENTREGADO", "DELIVERED"));
    }

    @Test
    void soloDevuelveEnviosQueAunNoHanSidoEntregados() {
        // Arrange
        entityManager.flush();
        entityManager.clear();

        // Act
        var resultado = enviosEnRedRepository.buscarEnRed();

        // Assert
        assertThat(resultado)
                .extracting(ShipmentTrackingView::getTrackingNumber)
                .containsExactly("TRACK-EN-RED");
    }

    private ShipmentTrackingView crearEnvio(String trackingNumber, String status) {
        return new ShipmentTrackingView(
                trackingNumber,
                status,
                new ShipmentTrackingView.Origen("Remitente", 10L, "Bogota"),
                new ShipmentTrackingView.Destino("Destinatario", "Calle 1", 20L, "Medellin"),
                Instant.parse("2026-10-01T10:00:00Z"));
    }
}
