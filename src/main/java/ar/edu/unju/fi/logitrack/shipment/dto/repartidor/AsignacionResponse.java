package ar.edu.unju.fi.logitrack.shipment.dto.repartidor;

import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Resultado de la asignación de un repartidor a un envío")
public record AsignacionResponse(
        UUID envioId,
        String codigoSeguimiento,
        EstadoEnvio estadoEnvio,
        RepartidorResponse repartidor
) {
}
