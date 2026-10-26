package ar.edu.unju.fi.logitrack.shipment.dto.asignacion;

import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Resultado de la asignación de un repartidor a un envío")
public record AsignacionResponse(
        @Schema(description = "Identificador del envío")
        UUID envioId,
        @Schema(description = "Código de seguimiento del envío")
        String codigoSeguimiento,
        @Schema(description = "Estado actual del envío")
        EstadoEnvio estadoEnvio,
        @Schema(description = "Identificador del repartidor asignado (vive en courier-service)")
        UUID repartidorId
) {
}
