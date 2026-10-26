package ar.edu.unju.fi.logitrack.shipment.dto.envio;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datos que se reciben para registrar un envío nuevo.
 * <p>
 * El cliente se indica con el {@code clienteId} de un cliente existente o mandando sus datos en
 * {@code cliente}. Si llegan los dos, se usa el {@code clienteId}.
 * </p>
 */
public record RegistrarEnvioRequest(

    @Schema(description = "Id de un cliente ya registrado. Puede quedar vacío si se mandan los datos del cliente.")
    UUID clienteId,

    @NotBlank @Size(max = 120) String origenCalle,
    @NotBlank @Size(max = 10) String origenNumero,
    @NotBlank @Size(max = 80) String origenCiudad,
    @NotBlank @Size(max = 10) String origenCodigoPostal,

    @NotBlank @Size(max = 120) String destinoCalle,
    @NotBlank @Size(max = 10) String destinoNumero,
    @NotBlank @Size(max = 80) String destinoCiudad,
    @NotBlank @Size(max = 10) String destinoCodigoPostal,

    @Schema(description = "Peso real en kilos", example = "2.5")
    @NotNull @DecimalMin(value = "0.001") @Digits(integer = 7, fraction = 3) BigDecimal peso,

    @Schema(description = "Ancho en centímetros", example = "30")
    @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal ancho,

    @Schema(description = "Alto en centímetros", example = "20")
    @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal alto,

    @Schema(description = "Largo en centímetros", example = "40")
    @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal largo,

    @Schema(description = "Datos del cliente. Se usan cuando no se manda el clienteId.")
    @Valid DatosClienteRequest cliente
) {}
