package ar.edu.unju.fi.logitrack.shipment.dto.envio;

import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;

import java.time.Instant;

/**
 * Un registro del historial de auditoría de un envío.
 *
 * @param estadoAnterior estado previo al cambio, nulo en el registro inicial de creación
 * @param estadoNuevo    estado al que pasó el envío
 * @param fechaHora      momento exacto en que ocurrió el cambio
 */
public record HistorialEstadoResponse(
    EstadoEnvio estadoAnterior,
    EstadoEnvio estadoNuevo,
    Instant fechaHora
) {}
