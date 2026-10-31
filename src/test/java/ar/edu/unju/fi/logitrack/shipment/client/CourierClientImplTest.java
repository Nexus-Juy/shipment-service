package ar.edu.unju.fi.logitrack.shipment.client;

import ar.edu.unju.fi.logitrack.shipment.dto.courier.DisponibilidadResponse;
import ar.edu.unju.fi.logitrack.shipment.exception.CourierServiceUnavailableException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Verifica la traducción de errores de {@link CourierClientImpl} sobre {@link CourierFeignClient}.
 * <p>
 * El camino HTTP real (serialización, 200/404, relay del JWT) lo cubre
 * {@code FlujoCompletoHttpE2ETest}, que levanta un courier-service falso con {@code HttpServer}.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CourierClientImplTest {

    private final UUID repartidorId = UUID.fromString("3f6c1a52-8e0b-4d7a-9c11-0a1b2c3d4e01");

    @Mock
    private CourierFeignClient courierFeignClient;

    @InjectMocks
    private CourierClientImpl client;

    @Test
    @DisplayName("200: devuelve la disponibilidad que entrega courier-service")
    void devuelveDisponibilidadDisponible() {
        var esperada = new DisponibilidadResponse(repartidorId, "ACTIVO", true);
        when(courierFeignClient.disponibilidad(repartidorId)).thenReturn(esperada);

        DisponibilidadResponse respuesta = client.consultarDisponibilidad(repartidorId);

        assertThat(respuesta).isEqualTo(esperada);
    }

    @Test
    @DisplayName("200: un repartidor INACTIVO llega con disponible=false")
    void devuelveDisponibilidadNoDisponible() {
        when(courierFeignClient.disponibilidad(repartidorId))
                .thenReturn(new DisponibilidadResponse(repartidorId, "INACTIVO", false));

        DisponibilidadResponse respuesta = client.consultarDisponibilidad(repartidorId);

        assertThat(respuesta.disponible()).isFalse();
        assertThat(respuesta.estado()).isEqualTo("INACTIVO");
    }

    @Test
    @DisplayName("404: courier-service no conoce al repartidor y se traduce a ResourceNotFoundException")
    void traduce404ARecursoNoEncontrado() {
        when(courierFeignClient.disponibilidad(repartidorId))
                .thenThrow(new FeignException.NotFound("404", peticion(), null, Map.of()));

        assertThatThrownBy(() -> client.consultarDisponibilidad(repartidorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(repartidorId.toString());
    }

    @Test
    @DisplayName("500: un error de courier-service se traduce a 503")
    void traduce5xxAServicioNoDisponible() {
        when(courierFeignClient.disponibilidad(repartidorId))
                .thenThrow(new FeignException.InternalServerError("500", peticion(), null, Map.of()));

        assertThatThrownBy(() -> client.consultarDisponibilidad(repartidorId))
                .isInstanceOf(CourierServiceUnavailableException.class)
                .hasMessageContaining("500");
    }

    @Test
    @DisplayName("Fallo de conexión: se traduce a 503")
    void traduceFalloDeConexionAServicioNoDisponible() {
        when(courierFeignClient.disponibilidad(repartidorId))
                .thenThrow(new RetryableException(-1, "connection refused", Request.HttpMethod.GET,
                        new IOException("connection refused"), (Long) null, peticion()));

        assertThatThrownBy(() -> client.consultarDisponibilidad(repartidorId))
                .isInstanceOf(CourierServiceUnavailableException.class)
                .hasMessageContaining("No se pudo consultar courier-service");
    }

    @Test
    @DisplayName("Respuesta vacía: se traduce a 503 en lugar de devolver null")
    void traduceRespuestaVaciaAServicioNoDisponible() {
        when(courierFeignClient.disponibilidad(repartidorId)).thenReturn(null);

        assertThatThrownBy(() -> client.consultarDisponibilidad(repartidorId))
                .isInstanceOf(CourierServiceUnavailableException.class);
    }

    private static Request peticion() {
        return Request.create(Request.HttpMethod.GET, "http://courier-service:8082/api/v1/repartidores/x/disponibilidad",
                Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
    }
}
