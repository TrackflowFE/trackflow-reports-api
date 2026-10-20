package com.trackflow.modules.reports.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

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

    private record Acceso(Function<ShipmentTransitView, Instant> leer,
                          BiConsumer<ShipmentTransitView, Instant> escribir) {
    }

    // La tabla comparte la correspondencia sin cambiar los campos persistidos por JPA.
    private static final Map<HitoDelRecorrido, Acceso> HITOS = Map.of(
            HitoDelRecorrido.RECEIVED_AT_CENTER,
            new Acceso(ShipmentTransitView::getReceivedAt, (view, at) -> view.receivedAt = at),
            HitoDelRecorrido.DISPATCHED,
            new Acceso(ShipmentTransitView::getDispatchedAt, (view, at) -> view.dispatchedAt = at),
            HitoDelRecorrido.ARRIVED_AT_DESTINATION_CENTER,
            new Acceso(ShipmentTransitView::getArrivedAt, (view, at) -> view.arrivedAt = at),
            HitoDelRecorrido.OUT_FOR_DELIVERY,
            new Acceso(ShipmentTransitView::getOutForDeliveryAt, (view, at) -> view.outForDeliveryAt = at),
            HitoDelRecorrido.DELIVERED,
            new Acceso(ShipmentTransitView::getDeliveredAt, (view, at) -> view.deliveredAt = at));

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

        HITOS.get(hito).escribir().accept(this, at);
        return true;
    }

    public Instant get(HitoDelRecorrido hito) {
        return HITOS.get(hito).leer().apply(this);
    }

    public String getTrackingNumber() { return trackingNumber; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getDispatchedAt() { return dispatchedAt; }
    public Instant getArrivedAt() { return arrivedAt; }
    public Instant getOutForDeliveryAt() { return outForDeliveryAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
}
