package com.trackflow.modules.reports.infrastructure;

import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentTrackingViewJpaRepository extends JpaRepository<ShipmentTrackingView, String> {
}