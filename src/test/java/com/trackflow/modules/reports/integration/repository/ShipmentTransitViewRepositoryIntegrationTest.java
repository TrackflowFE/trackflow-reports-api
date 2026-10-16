package com.trackflow.modules.reports.integration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.trackflow.modules.reports.application.ShipmentTransitViewRepository;
import com.trackflow.modules.reports.application.ShipmentTransitViewRepository.Medicion;
import com.trackflow.modules.reports.domain.HitoDelRecorrido;
import com.trackflow.modules.reports.domain.ShipmentTransitView;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShipmentTransitViewRepositoryIntegrationTest {

    private static final Instant DESDE = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant HASTA = Instant.parse("2026-10-02T00:00:00Z");

    @Autowired
    private ShipmentTransitViewRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void cuentaEntregasYCalculaPromediosDeRecorridoSoloEnElRango() {
        // Arrange
        ShipmentTransitView completo = new ShipmentTransitView("TRANSIT-COMPLETO");
        registrar(completo, HitoDelRecorrido.RECEIVED_AT_CENTER, "2026-10-01T00:00:00Z");
        registrar(completo, HitoDelRecorrido.DISPATCHED, "2026-10-01T01:00:00Z");
        registrar(completo, HitoDelRecorrido.ARRIVED_AT_DESTINATION_CENTER, "2026-10-01T03:00:00Z");
        registrar(completo, HitoDelRecorrido.OUT_FOR_DELIVERY, "2026-10-01T04:00:00Z");
        registrar(completo, HitoDelRecorrido.DELIVERED, "2026-10-01T06:00:00Z");
        repository.save(completo);

        ShipmentTransitView parcial = new ShipmentTransitView("TRANSIT-PARCIAL");
        registrar(parcial, HitoDelRecorrido.RECEIVED_AT_CENTER, "2026-10-01T02:00:00Z");
        registrar(parcial, HitoDelRecorrido.DISPATCHED, "2026-10-01T04:00:00Z");
        registrar(parcial, HitoDelRecorrido.DELIVERED, "2026-10-01T10:00:00Z");
        repository.save(parcial);

        ShipmentTransitView fueraDeRango = new ShipmentTransitView("TRANSIT-FRONTERA");
        registrar(fueraDeRango, HitoDelRecorrido.RECEIVED_AT_CENTER, "2026-10-01T00:00:00Z");
        registrar(fueraDeRango, HitoDelRecorrido.DELIVERED, HASTA.toString());
        repository.save(fueraDeRango);
        entityManager.flush();
        entityManager.clear();

        // Act
        long entregados = repository.contarEntregadosEntre(DESDE, HASTA);
        Map<String, Medicion> mediciones = repository.medirEntregadosEntre(DESDE, HASTA);

        // Assert
        assertThat(entregados).isEqualTo(2);
        assertThat(mediciones).containsEntry(
                ShipmentTransitViewRepository.TOTAL, new Medicion(Duration.ofHours(7), 2));
        assertThat(mediciones).containsEntry("ORIGIN_PROCESSING", new Medicion(Duration.ofMinutes(90), 2));
        assertThat(mediciones).containsEntry("TRANSPORT", new Medicion(Duration.ofHours(2), 1));
        assertThat(mediciones).containsEntry("DESTINATION_PROCESSING", new Medicion(Duration.ofHours(1), 1));
        assertThat(mediciones).containsEntry("LAST_MILE", new Medicion(Duration.ofHours(2), 1));
    }

    private void registrar(ShipmentTransitView view, HitoDelRecorrido hito, String at) {
        view.registrar(hito, Instant.parse(at));
    }
}
