package com.trackflow.modules.reports.application;

import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import com.trackflow.modules.reports.domain.TrackingNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarEstadoEnvio {

    private final ShipmentTrackingViewRepository repository;

    public ConsultarEstadoEnvio(ShipmentTrackingViewRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ShipmentTrackingView ejecutar(String trackingNumber) {
        return repository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new TrackingNotFoundException(trackingNumber));
    }
}