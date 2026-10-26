package ar.edu.unju.fi.logitrack.shipment.dto.repartidor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Petición para asignar un repartidor a un envío")
public record AsignacionRequest(
        @Schema(description = "Identificador único del repartidor", example = "3f6c1a52-8e0b-4d7a-9c11-0a1b2c3d4e01")
        @NotNull(message = "El identificador del repartidor es obligatorio")
        UUID repartidorId
) {
}
