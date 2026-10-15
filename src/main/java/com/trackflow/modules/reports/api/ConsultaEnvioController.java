package com.trackflow.modules.reports.api;

import com.trackflow.modules.reports.api.dto.TrackingResponse;
import com.trackflow.modules.reports.application.ConsultarEstadoEnvio;
import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracking")
public class ConsultaEnvioController {

    private final ConsultarEstadoEnvio consultarEstadoEnvio;

    public ConsultaEnvioController(ConsultarEstadoEnvio consultarEstadoEnvio) {
        this.consultarEstadoEnvio = consultarEstadoEnvio;
    }

    @GetMapping("/{trackingNumber}")
    public ResponseEntity<TrackingResponse> consultar(@PathVariable String trackingNumber) {
        ShipmentTrackingView view = consultarEstadoEnvio.ejecutar(trackingNumber);
        // La dirección solo se expone una vez entregado: antes de eso el paquete no
        // ha llegado ahí, así que mostrarla no tiene sentido.
        String direccionSiEntregado = "DELIVERED".equals(view.getStatus()) ? view.getRecipientAddress() : null;

        return ResponseEntity.ok(new TrackingResponse(
                view.getTrackingNumber(),
                view.getStatus(),
                view.getSenderName(),
                view.getOriginCity(),
                view.getRecipientName(),
                view.getDestinationCityId(),
                view.getDestinationCity(),
                view.getRegisteredAt(),
                view.getLastMovementPoint(),
                view.getLastMovementAt(),
                view.getLastMovementCityId() != null ? view.getLastMovementCityId() : view.getOriginCityId(),
                view.getLastMovementCity() != null ? view.getLastMovementCity() : view.getOriginCity(),
                direccionSiEntregado
        ));
    }
}
