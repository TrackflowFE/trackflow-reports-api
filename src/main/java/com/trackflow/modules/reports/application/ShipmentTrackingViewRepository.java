package com.trackflow.modules.reports.application;

import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import java.util.Optional;

public interface ShipmentTrackingViewRepository {

    ShipmentTrackingView save(ShipmentTrackingView view);

    Optional<ShipmentTrackingView> findByTrackingNumber(String trackingNumber);

    boolean exists(String trackingNumber);
}