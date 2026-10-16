package com.trackflow.modules.reports.integration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.trackflow.modules.reports.application.ShipmentVolumeViewRepository;
import com.trackflow.modules.reports.application.VolumenPorPunto;
import com.trackflow.modules.reports.domain.ShipmentVolumeView;
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
class ShipmentVolumeViewRepositoryIntegrationTest {

    private static final Instant DESDE = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant HASTA = Instant.parse("2026-10-02T00:00:00Z");

    @Autowired
    private ShipmentVolumeViewRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void agrupaIngresosYPendientesYRespetaElRangoSemiabierto() {
        // Arrange
        ShipmentVolumeView recibidoUno = crearEnvio("VOLUME-1", "Bogota", "2026-10-01T01:00:00Z");
        recibidoUno.registrarIngreso(new ShipmentVolumeView.Ingreso(10L, "Centro Norte", 1L, "Bogota",
                Instant.parse("2026-10-01T02:00:00Z")));
        recibidoUno.registrarEntrega(Instant.parse("2026-10-01T12:00:00Z"));
        repository.save(recibidoUno);

        ShipmentVolumeView recibidoDos = crearEnvio("VOLUME-2", "Bogota", "2026-10-01T03:00:00Z");
        recibidoDos.registrarIngreso(new ShipmentVolumeView.Ingreso(10L, "Centro Norte", 1L, "Bogota",
                Instant.parse("2026-10-01T04:00:00Z")));
        recibidoDos.registrarEntrega(HASTA);
        repository.save(recibidoDos);

        ShipmentVolumeView sinPuntoIdentificado =
                crearEnvio("VOLUME-3", "Medellin", "2026-10-01T05:00:00Z");
        sinPuntoIdentificado.registrarIngreso(new ShipmentVolumeView.Ingreso(
                null, null, null, "Medellin", Instant.parse("2026-10-01T06:00:00Z")));
        repository.save(sinPuntoIdentificado);

        repository.save(crearEnvio("VOLUME-4", "Cali", "2026-10-01T07:00:00Z"));
        repository.save(crearEnvio("VOLUME-5", "Cali", "2026-10-01T08:00:00Z"));
        repository.save(crearEnvio("VOLUME-FRONTERA", "Cali", HASTA.toString()));
        entityManager.flush();
        entityManager.clear();

        // Act
        var porPunto = repository.registradosPorPuntoDeIngreso(DESDE, HASTA);
        var pendientes = repository.registradosPendientesDeRecepcion(DESDE, HASTA);
        long entregados = repository.entregadosEntre(DESDE, HASTA);

        // Assert
        assertThat(porPunto).containsExactlyInAnyOrder(
                new VolumenPorPunto(10L, "Centro Norte", "Bogota", false, 2),
                new VolumenPorPunto(null, "Punto sin identificar", "Medellin", false, 1));
        assertThat(pendientes).containsExactly(
                new VolumenPorPunto(null, "Pendiente de recepción en Cali", "Cali", true, 2));
        assertThat(entregados).isEqualTo(1);
    }

    private ShipmentVolumeView crearEnvio(String trackingNumber, String city, String registeredAt) {
        return new ShipmentVolumeView(
                trackingNumber, Instant.parse(registeredAt), 1L, city);
    }
}
