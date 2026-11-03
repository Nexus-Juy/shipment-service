package ar.edu.unju.fi.logitrack.shipment.client;

import ar.edu.unju.fi.logitrack.shipment.dto.courier.DisponibilidadResponse;
import ar.edu.unju.fi.logitrack.shipment.exception.CourierServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * {@link CourierClientImpl} solo delega en el cliente Feign y le pasa el JWT de la petición en curso.
 * <p>
 * La traducción de errores se prueba en {@link CourierErrorDecoderTest} y
 * {@link CourierFeignFallbackFactoryTest}; el camino HTTP real (circuit breaker incluido), en
 * {@code FlujoCompletoHttpE2ETest} y {@code AsignacionResilienciaE2ETest}.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CourierClientImplTest {

    private final UUID repartidorId = UUID.fromString("3f6c1a52-8e0b-4d7a-9c11-0a1b2c3d4e01");

    @Mock
    private CourierFeignClient courierFeignClient;

    @InjectMocks
    private CourierClientImpl client;

    @AfterEach
    void limpiarPeticion() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Devuelve la disponibilidad que entrega courier-service")
    void devuelveLaDisponibilidad() {
        var esperada = new DisponibilidadResponse(repartidorId, "ACTIVO", true);
        when(courierFeignClient.disponibilidad(repartidorId, null)).thenReturn(esperada);

        assertThat(client.consultarDisponibilidad(repartidorId)).isEqualTo(esperada);
    }

    @Test
    @DisplayName("Reenvía el JWT de la petición HTTP en curso")
    void reenviaElJwtDeLaPeticion() {
        var peticion = new MockHttpServletRequest();
        peticion.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-del-usuario");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(peticion));
        var esperada = new DisponibilidadResponse(repartidorId, "ACTIVO", true);
        when(courierFeignClient.disponibilidad(repartidorId, "Bearer token-del-usuario")).thenReturn(esperada);

        assertThat(client.consultarDisponibilidad(repartidorId)).isEqualTo(esperada);
    }

    @Test
    @DisplayName("Respuesta vacía: se traduce a 503 en lugar de devolver null")
    void respuestaVaciaEsServicioNoDisponible() {
        when(courierFeignClient.disponibilidad(repartidorId, null)).thenReturn(null);

        assertThatThrownBy(() -> client.consultarDisponibilidad(repartidorId))
                .isInstanceOf(CourierServiceUnavailableException.class);
    }
}
