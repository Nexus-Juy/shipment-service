package ar.edu.unju.fi.logitrack.shipment.service;

import ar.edu.unju.fi.logitrack.shipment.dto.repartidor.RepartidorRequest;
import ar.edu.unju.fi.logitrack.shipment.dto.repartidor.RepartidorResponse;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoRepartidor;

import java.util.List;
import java.util.UUID;

/**
 * Contrato de la gestión de repartidores.
 */
public interface RepartidorService {

    /** Registra un repartidor ACTIVO; el email debe ser único. */
    RepartidorResponse registrar(RepartidorRequest request);

    /** @param estado filtro opcional; si es nulo devuelve todos. */
    List<RepartidorResponse> listar(EstadoRepartidor estado);

    RepartidorResponse obtenerPorId(UUID id);

    /** Reemplaza nombre, vehículo, email y teléfono; el email no puede pertenecer a otro repartidor. */
    RepartidorResponse actualizar(UUID id, RepartidorRequest request);

    /**
     * Activa o desactiva un repartidor.
     * Regla de negocio: no puede desactivarse si tiene envíos EN_RUTA.
     */
    RepartidorResponse cambiarEstado(UUID id, EstadoRepartidor nuevoEstado);
}
