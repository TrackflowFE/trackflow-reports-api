package com.trackflow.modules.reports.infrastructure;

import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnviosEnRedJpaRepository extends JpaRepository<ShipmentTrackingView, String> {

    List<ShipmentTrackingView> findByStatusNot(String status);
}
