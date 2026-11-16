package com.trackflow.modules.reports.api;

import com.trackflow.modules.reports.api.dto.NombreEnmascarado;
import com.trackflow.modules.reports.api.dto.TrackingResponse;
import com.trackflow.modules.reports.application.ConsultarEstadoEnvio;
import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rastreo público: el número de seguimiento es la credencial del cliente, así que la
 * ruta no exige sesión. Un anónimo ve el estado y los puntos del recorrido, pero los
 * nombres van enmascarados y la dirección nunca se publica; un operador con sesión ve
 * la respuesta completa.
 */
@RestController
@RequestMapping("/api/tracking")
public class ConsultaEnvioController {

    private final ConsultarEstadoEnvio consultarEstadoEnvio;

    public ConsultaEnvioController(ConsultarEstadoEnvio consultarEstadoEnvio) {
        this.consultarEstadoEnvio = consultarEstadoEnvio;
    }

    @GetMapping("/{trackingNumber}")
    public ResponseEntity<TrackingResponse> consultar(@PathVariable String trackingNumber,
            Authentication authentication) {
        ShipmentTrackingView view = consultarEstadoEnvio.ejecutar(trackingNumber);
        // El parámetro llega con la cadena de seguridad; el contexto cubre las pruebas
        // que montan el controlador sin filtros.
        Authentication sesion = authentication != null
                ? authentication : SecurityContextHolder.getContext().getAuthentication();
        boolean operador = esOperador(sesion);
        // La dirección solo se expone a un operador y una vez entregado: antes de eso el
        // paquete no ha llegado ahí, y a un anónimo no se le publica nunca.
        String direccionSiEntregado = operador && "DELIVERED".equals(view.getStatus())
                ? view.getRecipientAddress() : null;

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new TrackingResponse(
                        view.getTrackingNumber(),
                        view.getStatus(),
                        operador ? view.getSenderName() : NombreEnmascarado.de(view.getSenderName()),
                        view.getOriginCity(),
                        operador ? view.getRecipientName() : NombreEnmascarado.de(view.getRecipientName()),
                        view.getDestinationCityId(),
                        view.getDestinationCity(),
                        view.getRegisteredAt(),
                        view.getLastMovementPoint(),
                        view.getLastMovementAt(),
                        view.getLastMovementCityId() != null ? view.getLastMovementCityId() : view.getOriginCityId(),
                        view.getLastMovementCity() != null ? view.getLastMovementCity() : view.getOriginCity(),
                        direccionSiEntregado,
                        !operador));
    }

    private static boolean esOperador(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(autoridad -> "ROLE_OPERADOR".equals(autoridad) || "ROLE_ADMIN".equals(autoridad));
    }
}
