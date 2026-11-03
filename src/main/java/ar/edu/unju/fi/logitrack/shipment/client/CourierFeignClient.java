package ar.edu.unju.fi.logitrack.shipment.client;

import ar.edu.unju.fi.logitrack.shipment.dto.courier.DisponibilidadResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

/**
 * Cliente declarativo (Spring Cloud OpenFeign) de {@code courier-service}.
 * <p>
 * No tiene URL: se resuelve por el <b>nombre lógico</b> {@code courier-service}, que es el nombre con el
 * que courier-service se registra en Eureka (S3-03).
 * </p>
 * <p>
 * Tolerancia a fallos (S3-04): los errores HTTP los traduce {@link CourierErrorDecoder} y, cuando la
 * llamada falla (courier-service caído, timeout, sin instancias en Eureka o circuito abierto), responde
 * {@link CourierFeignFallbackFactory}. En los dos casos el resultado es una excepción de dominio que el
 * {@code GlobalExceptionHandler} convierte en un {@code ProblemDetail} (404 o 503), nunca un 500.
 * </p>
 * <p>
 * El JWT del usuario viaja como parámetro y no con un interceptor: el circuit breaker puede ejecutar la
 * llamada en otro hilo, donde ya no está disponible la petición HTTP original.
 * </p>
 */
@FeignClient(
        name = "courier-service",
        configuration = CourierFeignConfig.class,
        fallbackFactory = CourierFeignFallbackFactory.class)
public interface CourierFeignClient {

    @GetMapping("/api/v1/repartidores/{id}/disponibilidad")
    DisponibilidadResponse disponibilidad(
            @PathVariable("id") UUID repartidorId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);
}
