package ar.edu.unju.fi.logitrack.shipment.dto.envio;

import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Datos de un envío que devuelve la API.
 *
 * @param id                identificador del envío
 * @param codigoSeguimiento código que usa el cliente para seguir su paquete
 * @param estado            estado actual del envío
 * @param clienteId         id del cliente que mandó el paquete
 * @param repartidorId      id del repartidor asignado, o nulo si todavía no tiene
 * @param origen            dirección de origen
 * @param destino           dirección de destino
 * @param pesoKg            peso real del paquete en kilos
 * @param altoCm            alto del paquete en centímetros
 * @param anchoCm           ancho del paquete en centímetros
 * @param largoCm           largo del paquete en centímetros
 * @param tarifa            precio que se le cobra al cliente por el envío
 * @param createdAt         fecha y hora de registro del envío
 */
public record EnvioResponse(
    UUID id,
    String codigoSeguimiento,
    EstadoEnvio estado,
    UUID clienteId,
    UUID repartidorId,
    DireccionResponse origen,
    DireccionResponse destino,
    BigDecimal pesoKg,
    BigDecimal altoCm,
    BigDecimal anchoCm,
    BigDecimal largoCm,
    BigDecimal tarifa,
    Instant createdAt
) {}
