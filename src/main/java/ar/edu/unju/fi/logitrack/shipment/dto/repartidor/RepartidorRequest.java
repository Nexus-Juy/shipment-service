package ar.edu.unju.fi.logitrack.shipment.dto.repartidor;

import ar.edu.unju.fi.logitrack.shipment.entity.enums.TipoVehiculo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para registrar o actualizar un repartidor")
public record RepartidorRequest(

        @Schema(example = "Juan Pérez")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String nombre,

        @Schema(example = "MOTO")
        @NotNull(message = "El tipo de vehículo es obligatorio")
        TipoVehiculo tipoVehiculo,

        @Schema(description = "Correo de contacto, único por repartidor", example = "juan.perez@logitrack.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 150, message = "El email no puede superar los 150 caracteres")
        String email,

        @Schema(example = "+54 388 400-2000")
        @NotBlank(message = "El teléfono es obligatorio")
        @Size(min = 6, max = 30, message = "El teléfono debe tener entre 6 y 30 caracteres")
        String telefono
) {
}
