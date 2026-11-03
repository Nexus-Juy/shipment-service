package ar.edu.unju.fi.logitrack.shipment.client;

import ar.edu.unju.fi.logitrack.shipment.exception.CourierServiceUnavailableException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourierFeignFallbackFactoryTest {

    private static final UUID ID = UUID.fromString("3f6c1a52-8e0b-4d7a-9c11-0a1b2c3d4e01");

    private final CourierFeignFallbackFactory factory = new CourierFeignFallbackFactory();

    private void llamarConFalla(Throwable causa) {
        factory.create(causa).disponibilidad(ID, "Bearer x");
    }

    @Test
    @DisplayName("Un 404 que ya tradujo el ErrorDecoder se deja pasar: sigue siendo 404")
    void dejaPasarElNoEncontrado() {
        var original = ResourceNotFoundException.of("Repartidor", ID);

        assertThatThrownBy(() -> llamarConFalla(original)).isSameAs(original);
    }

    @Test
    @DisplayName("Un 503 que ya tradujo el ErrorDecoder se deja pasar")
    void dejaPasarElServicioNoDisponible() {
        var original = new CourierServiceUnavailableException("courier-service respondió con estado 500", null);

        assertThatThrownBy(() -> llamarConFalla(original)).isSameAs(original);
    }

    @Test
    @DisplayName("Conexión rechazada (courier caído): 503 con mensaje claro")
    void conexionRechazada() {
        Request peticion = Request.create(Request.HttpMethod.GET, "http://courier-service/x",
                Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
        var causa = new RetryableException(-1, "connection refused", Request.HttpMethod.GET,
                new IOException("connection refused"), (Long) null, peticion);

        assertThatThrownBy(() -> llamarConFalla(causa))
                .isInstanceOf(CourierServiceUnavailableException.class)
                .hasMessageContaining("No se pudo consultar courier-service");
    }

    @Test
    @DisplayName("Timeout: 503")
    void timeout() {
        assertThatThrownBy(() -> llamarConFalla(new TimeoutException("timeout")))
                .isInstanceOf(CourierServiceUnavailableException.class);
    }

    @Test
    @DisplayName("Sin instancias de courier-service en Eureka: 503")
    void sinInstancias() {
        assertThatThrownBy(() -> llamarConFalla(
                new IllegalStateException("Load balancer does not contain an instance for the service courier-service")))
                .isInstanceOf(CourierServiceUnavailableException.class)
                .hasMessageContaining("courier-service");
    }

    @Test
    @DisplayName("Circuito abierto: 503 sin llamar a courier-service")
    void circuitoAbierto() {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("courier");
        cb.transitionToOpenState();
        var causa = CallNotPermittedException.createCallNotPermittedException(cb);

        assertThatThrownBy(() -> llamarConFalla(causa))
                .isInstanceOf(CourierServiceUnavailableException.class)
                .hasMessageContaining("circuito abierto");
    }

    @Test
    @DisplayName("Si Resilience4j envuelve la excepción en CompletionException, se la desenvuelve")
    void desenvuelveLaCausaReal() {
        var original = ResourceNotFoundException.of("Repartidor", ID);

        assertThat(CourierFeignFallbackFactory.traducir(new CompletionException(original))).isSameAs(original);
    }
}
