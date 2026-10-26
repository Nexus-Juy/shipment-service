package ar.edu.unju.fi.logitrack.shipment.mapper;

import ar.edu.unju.fi.logitrack.shipment.dto.envio.DireccionResponse;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.EnvioResponse;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.HistorialEstadoResponse;
import ar.edu.unju.fi.logitrack.shipment.entity.Direccion;
import ar.edu.unju.fi.logitrack.shipment.entity.Envio;
import ar.edu.unju.fi.logitrack.shipment.entity.HistorialEstado;
import org.springframework.stereotype.Component;

@Component
public class EnvioMapper {

    public EnvioResponse toResponse(Envio envio) {
        return new EnvioResponse(
            envio.getId(),
            envio.getCodigoSeguimiento(),
            envio.getEstado(),
            envio.getCliente().getId(),
            envio.getRepartidorId(),
            toDireccionResponse(envio.getOrigen()),
            toDireccionResponse(envio.getDestino()),
            envio.getPesoKg(),
            envio.getAltoCm(),
            envio.getAnchoCm(),
            envio.getLargoCm(),
            envio.getTarifa(),
            envio.getCreatedAt()
        );
    }

    public HistorialEstadoResponse toHistorialResponse(HistorialEstado historial) {
        return new HistorialEstadoResponse(
            historial.getEstadoAnterior(),
            historial.getEstadoNuevo(),
            historial.getFechaHora()
        );
    }

    private DireccionResponse toDireccionResponse(Direccion direccion) {
        return new DireccionResponse(
            direccion.getCalle(),
            direccion.getNumero(),
            direccion.getCiudad(),
            direccion.getCodigoPostal()
        );
    }
}
