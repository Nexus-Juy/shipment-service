package ar.edu.unju.fi.logitrack.shipment.client;

import ar.edu.unju.fi.logitrack.shipment.dto.courier.DisponibilidadResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Cliente declarativo (Spring Cloud OpenFeign) de {@code courier-service}.
 * <p>
 * La URL base sale de {@code courier.service.url}, así el mismo código funciona contra
 * {@code http://localhost:8082} en desarrollo y contra {@code http://courier-service:8082} en Docker.
 * Los errores HTTP y de conexión llegan como {@link feign.FeignException}; los traduce
 * {@link CourierClientImpl}, que es el que usa el resto del servicio.
 * </p>
 */
@FeignClient(
        name = "courier-service",
        url = "${courier.service.url}",
        configuration = CourierFeignConfig.class)
public interface CourierFeignClient {

    @GetMapping("/api/v1/repartidores/{id}/disponibilidad")
    DisponibilidadResponse disponibilidad(@PathVariable("id") UUID repartidorId);
}
