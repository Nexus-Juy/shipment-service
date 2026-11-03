package ar.edu.unju.fi.logitrack.shipment.client;

import ar.edu.unju.fi.logitrack.shipment.dto.courier.DisponibilidadResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Cliente declarativo (Spring Cloud OpenFeign) de {@code courier-service}.
 * <p>
 * No tiene URL: se resuelve por el <b>nombre lógico</b> {@code courier-service}, que es el nombre con el
 * que courier-service se registra en Eureka. Spring Cloud LoadBalancer consulta a Eureka por las
 * instancias disponibles y elige una, así que shipment-service no conoce IP ni puerto de courier-service.
 * Los errores HTTP y de conexión llegan como {@link feign.FeignException}; los traduce
 * {@link CourierClientImpl}, que es el que usa el resto del servicio.
 * </p>
 */
@FeignClient(
        name = "courier-service",
        configuration = CourierFeignConfig.class)
public interface CourierFeignClient {

    @GetMapping("/api/v1/repartidores/{id}/disponibilidad")
    DisponibilidadResponse disponibilidad(@PathVariable("id") UUID repartidorId);
}
