package com.trackflow.modules.reports.infrastructure.messaging;

import com.trackflow.modules.reports.application.ShipmentTrackingViewRepository;
import com.trackflow.modules.reports.application.ShipmentTransitViewRepository;
import com.trackflow.modules.reports.application.ShipmentVolumeViewRepository;
import com.trackflow.shared.events.EnvioCreadoEvent;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import java.util.Objects;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReportsIntegrationEventListener {

    private final ApplicationEventPublisher projections;
    private final ShipmentTrackingViewRepository tracking;
    private final ShipmentTransitViewRepository transit;
    private final ShipmentVolumeViewRepository volume;

    public ReportsIntegrationEventListener(ApplicationEventPublisher projections,
            ShipmentTrackingViewRepository tracking, ShipmentTransitViewRepository transit,
            ShipmentVolumeViewRepository volume) {
        this.projections = projections;
        this.tracking = tracking;
        this.transit = transit;
        this.volume = volume;
    }

    @RabbitListener(queues = "${trackflow.reports.events.shipment-created-queue}")
    @Transactional
    public void onShipmentCreated(EnvioCreadoEvent event) {
        requireText(event.trackingNumber(), "trackingNumber");
        requireText(event.status(), "status");
        requireText(event.senderName(), "senderName");
        requireText(event.originCity(), "originCity");
        requireText(event.recipientName(), "recipientName");
        requireText(event.recipientAddress(), "recipientAddress");
        requireText(event.destinationCity(), "destinationCity");
        Objects.requireNonNull(event.originCityId(), "originCityId");
        Objects.requireNonNull(event.destinationCityId(), "destinationCityId");
        Objects.requireNonNull(event.registeredAt(), "registeredAt");
        Objects.requireNonNull(event.occurredAt(), "occurredAt");
        // Dispatch interno y sincrono: el ACK llega solo tras confirmar las tres filas.
        projections.publishEvent(event);
    }

    @RabbitListener(queues = "${trackflow.reports.events.logistics-registered-queue}")
    @Transactional
    public void onLogisticsRegistered(EventoLogisticoRegistradoEvent event) {
        requireText(event.trackingNumber(), "trackingNumber");
        requireText(event.eventType(), "eventType");
        requireText(event.resultingStatus(), "resultingStatus");
        requireText(event.point(), "point");
        Objects.requireNonNull(event.logisticsEventId(), "logisticsEventId");
        Objects.requireNonNull(event.movedAt(), "movedAt");
        Objects.requireNonNull(event.registeredAt(), "registeredAt");
        Objects.requireNonNull(event.occurredAt(), "occurredAt");
        // Colas independientes permiten que el alta avance mientras se reintenta el movimiento.
        if (tracking.findByTrackingNumber(event.trackingNumber()).isEmpty()
                || transit.findByTrackingNumber(event.trackingNumber()).isEmpty()
                || volume.findByTrackingNumber(event.trackingNumber()).isEmpty()) {
            throw new IllegalStateException("Alta pendiente de proyectar: " + event.trackingNumber());
        }
        projections.publishEvent(event);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campo requerido: " + field);
        }
    }
}
