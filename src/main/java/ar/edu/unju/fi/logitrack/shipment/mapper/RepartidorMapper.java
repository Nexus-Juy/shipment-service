package ar.edu.unju.fi.logitrack.shipment.mapper;

import ar.edu.unju.fi.logitrack.shipment.dto.repartidor.RepartidorRequest;
import ar.edu.unju.fi.logitrack.shipment.dto.repartidor.RepartidorResponse;
import ar.edu.unju.fi.logitrack.shipment.entity.Repartidor;
import org.springframework.stereotype.Component;

/** Convierte entre la entidad JPA y los DTOs de la API (la entidad nunca sale del servicio). */
@Component
public class RepartidorMapper {

    public Repartidor toEntity(RepartidorRequest request) {
        return new Repartidor(
                request.nombre().trim(),
                request.tipoVehiculo(),
                request.email().trim(),
                request.telefono().trim());
    }

    /** Devuelve {@code null} si no hay repartidor (por ejemplo, un envío todavía sin asignar). */
    public RepartidorResponse toResponse(Repartidor repartidor) {
        if (repartidor == null) {
            return null;
        }
        return new RepartidorResponse(
                repartidor.getId(),
                repartidor.getNombre(),
                repartidor.getTipoVehiculo(),
                repartidor.getEmail(),
                repartidor.getTelefono(),
                repartidor.getEstado(),
                repartidor.getCreatedAt(),
                repartidor.getUpdatedAt());
    }
}
