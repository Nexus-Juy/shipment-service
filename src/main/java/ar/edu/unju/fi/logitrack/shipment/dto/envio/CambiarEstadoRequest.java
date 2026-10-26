package ar.edu.unju.fi.logitrack.shipment.dto.envio;

import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(
    @NotNull EstadoEnvio estado
) {}
