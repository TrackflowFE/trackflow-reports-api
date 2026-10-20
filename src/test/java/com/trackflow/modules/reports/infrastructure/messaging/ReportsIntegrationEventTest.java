package com.trackflow.modules.reports.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.*;

import com.trackflow.modules.reports.infrastructure.ShipmentTrackingViewJpaRepository;
import com.trackflow.modules.reports.infrastructure.ShipmentTransitViewJpaRepository;
import com.trackflow.modules.reports.infrastructure.ShipmentVolumeViewJpaRepository;
import com.trackflow.modules.reports.domain.HitoDelRecorrido;
import com.trackflow.modules.reports.domain.ShipmentTransitView;
import com.trackflow.shared.events.EnvioCreadoEvent;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportsIntegrationEventTest {
    @Autowired ReportsIntegrationEventListener listener;
    @Autowired ShipmentTrackingViewJpaRepository tracking;
    @Autowired ShipmentTransitViewJpaRepository transit;
    @Autowired ShipmentVolumeViewJpaRepository volume;
    @Autowired MessageConverter converter;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private final Instant registered = Instant.now().minusSeconds(4 * 86400);

    @BeforeEach
    void clear() {
        tracking.deleteAll();
        transit.deleteAll();
        volume.deleteAll();
    }

    @Test
    void jsonEventUpdatesThreeProjectionsAndTrackingAndDelayEndpoints() throws Exception {
        var event = created();
        var message = converter.toMessage(event, new MessageProperties());
        message.getMessageProperties().getHeaders().clear();
        message.getMessageProperties().setInferredArgumentType(EnvioCreadoEvent.class);
        listener.onShipmentCreated((EnvioCreadoEvent) converter.fromMessage(message));
        listener.onLogisticsRegistered(movement("DISPATCHED", "IN_TRANSIT", registered.plusSeconds(3600)));

        assertEquals(1, tracking.count());
        assertEquals(1, transit.count());
        assertEquals(1, volume.count());
        assertEquals(6, tracking.findById("TF-TEST").orElseThrow().getPlannedLegHours());
        mvc.perform(get("/api/tracking/TF-TEST"))
                .andExpect(status().isOk()).andExpect(jsonPath("status").value("IN_TRANSIT"));
        mvc.perform(get("/api/reports/delayed-shipments"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("TF-TEST")));
    }

    @Test
    void redeliveredCreationDoesNotEraseMovementsOrDuplicateVolume() {
        listener.onShipmentCreated(created());
        listener.onLogisticsRegistered(movement("DELIVERED", "DELIVERED", registered.plusSeconds(7200)));
        listener.onShipmentCreated(created());
        assertEquals("DELIVERED", tracking.findById("TF-TEST").orElseThrow().getStatus());
        assertNotNull(transit.findById("TF-TEST").orElseThrow().getDeliveredAt());
        assertNotNull(volume.findById("TF-TEST").orElseThrow().getDeliveredAt());
        assertEquals(1, volume.count());
    }

    @Test
    void movementBeforeCreationFailsForRetryInsteadOfBeingSilentlyLost() {
        assertThrows(IllegalStateException.class,
                () -> listener.onLogisticsRegistered(movement("DISPATCHED", "IN_TRANSIT", registered)));
        assertEquals(0, tracking.count());
        listener.onShipmentCreated(created());
        listener.onLogisticsRegistered(movement("DISPATCHED", "IN_TRANSIT", registered));
        assertEquals("IN_TRANSIT", tracking.findById("TF-TEST").orElseThrow().getStatus());
    }

    @Test
    void invalidCreationDoesNotPartiallyPersist() {
        var invalid = new EnvioCreadoEvent("TF-TEST", "REGISTERED", "Sender", null, "Origin",
                "Recipient", "Address", 2L, "Destination", registered, registered);
        assertThrows(NullPointerException.class, () -> listener.onShipmentCreated(invalid));
        assertEquals(0, tracking.count());
        assertEquals(0, volume.count());
        assertEquals(0, transit.count());
    }

    @Test
    void databaseFailureInOneProjectionRollsBackAllThree() {
        jdbc.execute("alter table reports_shipment_volume add constraint test_reject_origin check (origin_city_id <> 99)");
        try {
            var rejected = new EnvioCreadoEvent("TF-TEST", "REGISTERED", "Sender", 99L, "Origin",
                    "Recipient", "Address", 2L, "Destination", registered, registered);
            assertThrows(RuntimeException.class, () -> listener.onShipmentCreated(rejected));
            assertEquals(0, tracking.count());
            assertEquals(0, transit.count());
            assertEquals(0, volume.count());
        } finally {
            jdbc.execute("alter table reports_shipment_volume drop constraint test_reject_origin");
        }
    }

    @Test
    void olderMovementDoesNotRegressTracking() {
        listener.onShipmentCreated(created());
        listener.onLogisticsRegistered(movement("DELIVERED", "DELIVERED", registered.plusSeconds(7200)));
        listener.onLogisticsRegistered(movement("DISPATCHED", "IN_TRANSIT", registered.plusSeconds(3600)));
        assertEquals("DELIVERED", tracking.findById("TF-TEST").orElseThrow().getStatus());
    }

    @Test
    void cincoHitosPersistenYSeRecarganEnSusColumnas() {
        Instant at = Instant.parse("2026-10-01T12:00:00Z");
        var view = new ShipmentTransitView("TF-ROUNDTRIP");
        int index = 0;
        for (HitoDelRecorrido hito : HitoDelRecorrido.values()) {
            assertTrue(view.registrar(hito, at.plusSeconds(index++ * 60L)));
        }
        transit.saveAndFlush(view);

        var loaded = transit.findById("TF-ROUNDTRIP").orElseThrow();
        assertNotSame(view, loaded);
        assertEquals(at, loaded.getReceivedAt());
        assertEquals(at.plusSeconds(60), loaded.getDispatchedAt());
        assertEquals(at.plusSeconds(120), loaded.getArrivedAt());
        assertEquals(at.plusSeconds(180), loaded.getOutForDeliveryAt());
        assertEquals(at.plusSeconds(240), loaded.getDeliveredAt());
        index = 0;
        for (HitoDelRecorrido hito : HitoDelRecorrido.values()) {
            assertEquals(at.plusSeconds(index++ * 60L), loaded.get(hito));
        }
    }

    @Test
    void falloDeVolumenEnMovimientoRevierteLasTresActualizacionesYPermiteReintento() {
        listener.onShipmentCreated(created());
        Instant received = registered.truncatedTo(java.time.temporal.ChronoUnit.MICROS).plusSeconds(60);
        Instant delivered = received.plusSeconds(60);
        listener.onLogisticsRegistered(movement("RECEIVED_AT_CENTER", "AT_ORIGIN_CENTER", received));
        jdbc.execute("alter table reports_shipment_volume add constraint test_reject_delivery check (delivered_at is null)");
        try {
            assertThrows(RuntimeException.class,
                    () -> listener.onLogisticsRegistered(movement("DELIVERED", "DELIVERED", delivered)));
            var persistedTracking = tracking.findById("TF-TEST").orElseThrow();
            assertEquals("AT_ORIGIN_CENTER", persistedTracking.getStatus());
            assertEquals(received, persistedTracking.getLastMovementAt());
            assertEquals(received, transit.findById("TF-TEST").orElseThrow().getReceivedAt());
            assertNull(transit.findById("TF-TEST").orElseThrow().getDeliveredAt());
            assertEquals(received, volume.findById("TF-TEST").orElseThrow().getReceivedAt());
            assertNull(volume.findById("TF-TEST").orElseThrow().getDeliveredAt());
        } finally {
            jdbc.execute("alter table reports_shipment_volume drop constraint test_reject_delivery");
        }
        listener.onLogisticsRegistered(movement("DELIVERED", "DELIVERED", delivered));
        assertEquals("DELIVERED", tracking.findById("TF-TEST").orElseThrow().getStatus());
        assertEquals(delivered, transit.findById("TF-TEST").orElseThrow().getDeliveredAt());
        assertEquals(delivered, volume.findById("TF-TEST").orElseThrow().getDeliveredAt());
    }

    private EnvioCreadoEvent created() {
        return new EnvioCreadoEvent("TF-TEST", "REGISTERED", "Sender", 1L, "Origin",
                "Recipient", "Address", 2L, "Destination", registered, registered);
    }

    private EventoLogisticoRegistradoEvent movement(String type, String status, Instant movedAt) {
        return new EventoLogisticoRegistradoEvent(1L, "TF-TEST", type, status, "Center", 10L,
                1L, "Origin", movedAt, movedAt, movedAt, 2L, "Destination", 6);
    }
}
