package com.trackflow.modules.reports.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Modelo de lectura del reporte de tiempos de tránsito: una fila por envío con la hora
 * en que ocurrió cada hito del recorrido, sin datos personales.
 *
 * De cada hito se conserva el más temprano: los reportes pueden llegar fuera de orden
 * o reentregarse, y una repetición no debe mover el hito ya registrado.
 */
@Entity
@Table(name = "reports_shipment_transit")
public class ShipmentTransitView {

    @Id
    private String trackingNumber;

    @Column
    private Instant receivedAt;

    @Column
    private Instant dispatchedAt;

    @Column
    private Instant arrivedAt;

    @Column
    private Instant outForDeliveryAt;

    @Column
    private Instant deliveredAt;

    protected ShipmentTransitView() {
    }

    public ShipmentTransitView(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    /** @return false si ya había un hito anterior o igual y este se ignora */
    public boolean registrar(HitoDelRecorrido hito, Instant at) {
        Instant actual = get(hito);
        if (actual != null && !at.isBefore(actual)) {
            return false;
        }

        switch (hito) {
            case RECEIVED_AT_CENTER -> receivedAt = at;
            case DISPATCHED -> dispatchedAt = at;
            case ARRIVED_AT_DESTINATION_CENTER -> arrivedAt = at;
            case OUT_FOR_DELIVERY -> outForDeliveryAt = at;
            case DELIVERED -> deliveredAt = at;
        }
        return true;
    }

    public Instant get(HitoDelRecorrido hito) {
        return switch (hito) {
            case RECEIVED_AT_CENTER -> receivedAt;
            case DISPATCHED -> dispatchedAt;
            case ARRIVED_AT_DESTINATION_CENTER -> arrivedAt;
            case OUT_FOR_DELIVERY -> outForDeliveryAt;
            case DELIVERED -> deliveredAt;
        };
    }

    public String getTrackingNumber() { return trackingNumber; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getDispatchedAt() { return dispatchedAt; }
    public Instant getArrivedAt() { return arrivedAt; }
    public Instant getOutForDeliveryAt() { return outForDeliveryAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
}
