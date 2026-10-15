package com.trackflow.modules.reports.infrastructure;

import com.trackflow.modules.reports.application.EnviosEnRedRepository;
import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaEnviosEnRedRepository implements EnviosEnRedRepository {

    private static final String ENTREGADO = "DELIVERED";

    private final EnviosEnRedJpaRepository jpa;

    public JpaEnviosEnRedRepository(EnviosEnRedJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<ShipmentTrackingView> buscarEnRed() {
        return jpa.findByStatusNot(ENTREGADO);
    }
}
