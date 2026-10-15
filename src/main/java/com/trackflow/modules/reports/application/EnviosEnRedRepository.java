package com.trackflow.modules.reports.application;

import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import java.util.List;

public interface EnviosEnRedRepository {

    /** Envíos que todavía están en la red: todos los que no han sido entregados. */
    List<ShipmentTrackingView> buscarEnRed();
}
