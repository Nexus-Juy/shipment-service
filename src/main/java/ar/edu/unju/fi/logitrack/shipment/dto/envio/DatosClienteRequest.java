package ar.edu.unju.fi.logitrack.shipment.dto.envio;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos del cliente que se pueden mandar al registrar un envío.
 * <p>
 * Si ya hay un cliente registrado con el mismo email se usa ese, y si no hay ninguno se crea uno nuevo.
 * Así un mismo cliente no queda cargado dos veces.
 * </p>
 *
 * @param nombre nombre o razón social del cliente
 * @param email correo del cliente, es lo que se usa para saber si ya estaba registrado
 * @param telefono teléfono de contacto, no es obligatorio
 */
@Schema(description = "Datos del cliente que manda el paquete")
public record DatosClienteRequest(

    @Schema(example = "Tienda Norte SRL")
    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 120, message = "El nombre del cliente no puede tener más de 120 caracteres")
    String nombre,

    @Schema(example = "ventas@tiendanorte.com")
    @NotBlank(message = "El email del cliente es obligatorio")
    @Email(message = "El email del cliente no tiene un formato válido")
    @Size(max = 150, message = "El email del cliente no puede tener más de 150 caracteres")
    String email,

    @Schema(example = "+54 388 400-1000")
    @Size(max = 30, message = "El teléfono del cliente no puede tener más de 30 caracteres")
    String telefono
) {
}
