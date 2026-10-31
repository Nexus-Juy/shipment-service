package ar.edu.unju.fi.logitrack.shipment.client;

import ar.edu.unju.fi.logitrack.shipment.dto.courier.DisponibilidadResponse;
import ar.edu.unju.fi.logitrack.shipment.exception.CourierServiceUnavailableException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementación de {@link CourierClient} sobre {@link CourierFeignClient} (OpenFeign).
 * <p>
 * Acá se centraliza la traducción de errores: un 404 de courier-service es "repartidor inexistente" y
 * cualquier otra falla (5xx, timeout, conexión rechazada, respuesta vacía) es "courier-service no
 * disponible", que el {@code GlobalExceptionHandler} responde con 503.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class CourierClientImpl implements CourierClient {

    private final CourierFeignClient courierFeignClient;

    @Override
    public DisponibilidadResponse consultarDisponibilidad(UUID repartidorId) {
        try {
            DisponibilidadResponse respuesta = courierFeignClient.disponibilidad(repartidorId);
            if (respuesta == null) {
                throw new CourierServiceUnavailableException(
                        "courier-service devolvió una respuesta vacía para el repartidor " + repartidorId, null);
            }
            return respuesta;
        } catch (FeignException.NotFound ex) {
            throw ResourceNotFoundException.of("Repartidor", repartidorId);
        } catch (FeignException ex) {
            // status() es -1 cuando no hubo respuesta HTTP (conexión rechazada, timeout).
            String mensaje = ex.status() > 0
                    ? "courier-service respondió con estado " + ex.status()
                    : "No se pudo consultar courier-service: " + ex.getMessage();
            throw new CourierServiceUnavailableException(mensaje, ex);
        }
    }
}
