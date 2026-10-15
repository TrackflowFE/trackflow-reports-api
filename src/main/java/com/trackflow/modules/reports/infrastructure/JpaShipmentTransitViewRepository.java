package com.trackflow.modules.reports.infrastructure;

import com.trackflow.modules.reports.application.ShipmentTransitViewRepository;
import com.trackflow.modules.reports.domain.ShipmentTransitView;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaShipmentTransitViewRepository implements ShipmentTransitViewRepository {

    private final ShipmentTransitViewJpaRepository jpa;

    public JpaShipmentTransitViewRepository(ShipmentTransitViewJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ShipmentTransitView save(ShipmentTransitView view) {
        return jpa.save(view);
    }

    @Override
    public Optional<ShipmentTransitView> findByTrackingNumber(String trackingNumber) {
        return jpa.findById(trackingNumber);
    }

    @Override
    public long contarEntregadosEntre(Instant desde, Instant hasta) {
        return jpa.contarEntregados(desde, hasta);
    }

    @Override
    public Map<String, Medicion> medirEntregadosEntre(Instant desde, Instant hasta) {
        Map<String, Medicion> mediciones = new HashMap<>();
        for (ShipmentTransitViewJpaRepository.TramoPromedio fila : jpa.promediarEntregados(desde, hasta)) {
            Duration promedio = fila.getPromedio() == null ? null : Duration.ofMillis(Math.round(fila.getPromedio() * 1000));
            mediciones.put(fila.getTramo(), new Medicion(promedio, fila.getEnvios()));
        }
        return mediciones;
    }
}
