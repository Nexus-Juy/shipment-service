package ar.edu.unju.fi.logitrack.shipment.mapper;

import ar.edu.unju.fi.logitrack.shipment.dto.asignacion.AsignacionResponse;
import ar.edu.unju.fi.logitrack.shipment.entity.Envio;
import org.springframework.stereotype.Component;

/**
 * Mapeador para armar la respuesta de la asignación de un repartidor a un envío.
 */
@Component
public class AsignacionMapper {

    public AsignacionResponse toResponse(Envio envio) {
        return new AsignacionResponse(
                envio.getId(),
                envio.getCodigoSeguimiento(),
                envio.getEstado(),
                envio.getRepartidorId()
        );
    }
}
