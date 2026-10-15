package com.trackflow.modules.reports.api.dto;

import java.time.Instant;

public record TrackingResponse(
        String trackingNumber,
        String status,
        String senderName,
        String originCity,
        String recipientName,
        Long destinationCityId,
        String destinationCity,
        Instant registeredAt,
        String lastMovementPoint,
        Instant lastMovementAt,

        /**
         * Ciudad donde está el paquete: la del último movimiento. Mientras no se haya
         * movido es la de origen, que es donde lo tiene el remitente.
         */
        Long currentCityId,
        String currentCity,

        /**
         * Dirección del destinatario. Solo viene informada cuando status es
         * DELIVERED: antes de eso el paquete no ha llegado ahí, así que mostrarla no
         * tiene sentido; en cualquier otro estado este campo es null.
         */
        String recipientAddress) {
}
