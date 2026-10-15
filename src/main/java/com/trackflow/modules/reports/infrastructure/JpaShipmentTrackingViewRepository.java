package com.trackflow.modules.reports.infrastructure;

import com.trackflow.modules.reports.application.ShipmentTrackingViewRepository;
import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaShipmentTrackingViewRepository implements ShipmentTrackingViewRepository {

    private final ShipmentTrackingViewJpaRepository jpa;

    public JpaShipmentTrackingViewRepository(ShipmentTrackingViewJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ShipmentTrackingView save(ShipmentTrackingView view) {
        return jpa.save(view);
    }

    @Override
    public Optional<ShipmentTrackingView> findByTrackingNumber(String trackingNumber) {
        return jpa.findById(trackingNumber);
    }

    @Override
    public boolean exists(String trackingNumber) {
        return jpa.existsById(trackingNumber);
    }
}