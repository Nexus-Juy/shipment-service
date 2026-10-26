package ar.edu.unju.fi.logitrack.shipment.dto.courier;

import java.util.UUID;

/**
 * Respuesta de {@code GET /api/v1/repartidores/{id}/disponibilidad} de courier-service.
 * <p>
 * {@code estado} se recibe como texto a propósito: shipment-service ya no conoce el enum
 * {@code EstadoRepartidor}, que es propiedad del microservicio de repartidores.
 * </p>
 */
public record DisponibilidadResponse(
        UUID repartidorId,
        String estado,
        boolean disponible
) {
}
