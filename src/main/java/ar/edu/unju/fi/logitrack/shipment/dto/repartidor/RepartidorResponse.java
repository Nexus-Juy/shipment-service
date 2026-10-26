package ar.edu.unju.fi.logitrack.shipment.dto.repartidor;

import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoRepartidor;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.TipoVehiculo;

import java.time.Instant;
import java.util.UUID;

public record RepartidorResponse(
        UUID id,
        String nombre,
        TipoVehiculo tipoVehiculo,
        String email,
        String telefono,
        EstadoRepartidor estado,
        Instant createdAt,
        Instant updatedAt
) {
}
