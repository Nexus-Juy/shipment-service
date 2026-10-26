package ar.edu.unju.fi.logitrack.shipment.service;

import ar.edu.unju.fi.logitrack.shipment.dto.asignacion.AsignacionResponse;

import java.util.UUID;

/**
 * Contrato del caso de uso de asignación de repartidores a envíos.
 */
public interface AsignacionService {

    /**
     * Asigna un repartidor a un envío. El envío debe estar PENDIENTE y el repartidor ACTIVO;
     * la disponibilidad se consulta por REST a courier-service, dueño de los repartidores.
     * Si el envío ya tenía un repartidor asignado, queda reasignado al nuevo.
     */
    AsignacionResponse asignarRepartidor(UUID envioId, UUID repartidorId);
}
