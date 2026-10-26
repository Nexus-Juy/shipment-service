package ar.edu.unju.fi.logitrack.shipment.dto.envio;

/**
 * Dirección de origen o destino de un envío.
 */
public record DireccionResponse(
    String calle,
    String numero,
    String ciudad,
    String codigoPostal
) {}
