package ar.edu.unju.fi.logitrack.shipment.client;

import ar.edu.unju.fi.logitrack.shipment.dto.courier.DisponibilidadResponse;

import java.util.UUID;

/**
 * Cliente del microservicio de repartidores. shipment-service lo consulta antes de asignar
 * un envío, porque la existencia y el estado del repartidor son responsabilidad de courier-service.
 */
public interface CourierClient {

    /**
     * Consulta la disponibilidad de un repartidor.
     *
     * @throws ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException si courier-service
     *         responde 404
     * @throws ar.edu.unju.fi.logitrack.shipment.exception.CourierServiceUnavailableException si
     *         courier-service no está accesible o responde con un error distinto de 404
     */
    DisponibilidadResponse consultarDisponibilidad(UUID repartidorId);
}
