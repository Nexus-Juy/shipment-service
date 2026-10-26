package ar.edu.unju.fi.logitrack.shipment.client;

import ar.edu.unju.fi.logitrack.shipment.dto.courier.DisponibilidadResponse;
import ar.edu.unju.fi.logitrack.shipment.exception.CourierServiceUnavailableException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Verifica el contrato HTTP de {@link CourierClientImpl} sin levantar courier-service:
 * {@link MockRestServiceServer} intercepta las llamadas de {@link RestClient}.
 */
class CourierClientImplTest {

    private static final String BASE_URL = "http://courier-service:8082";

    private final UUID repartidorId = UUID.fromString("3f6c1a52-8e0b-4d7a-9c11-0a1b2c3d4e01");
    private final String urlEsperada = BASE_URL + "/api/v1/repartidores/" + repartidorId + "/disponibilidad";

    private MockRestServiceServer server;
    private CourierClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CourierClientImpl(builder, BASE_URL);
    }

    @Test
    @DisplayName("200: mapea la disponibilidad que devuelve courier-service")
    void devuelveDisponibilidadDisponible() {
        server.expect(requestTo(urlEsperada))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"repartidorId": "%s", "estado": "ACTIVO", "disponible": true}
                        """.formatted(repartidorId), MediaType.APPLICATION_JSON));

        DisponibilidadResponse respuesta = client.consultarDisponibilidad(repartidorId);

        assertThat(respuesta.repartidorId()).isEqualTo(repartidorId);
        assertThat(respuesta.estado()).isEqualTo("ACTIVO");
        assertThat(respuesta.disponible()).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("200: un repartidor INACTIVO llega con disponible=false")
    void devuelveDisponibilidadNoDisponible() {
        server.expect(requestTo(urlEsperada))
                .andRespond(withSuccess("""
                        {"repartidorId": "%s", "estado": "INACTIVO", "disponible": false}
                        """.formatted(repartidorId), MediaType.APPLICATION_JSON));

        DisponibilidadResponse respuesta = client.consultarDisponibilidad(repartidorId);

        assertThat(respuesta.disponible()).isFalse();
        assertThat(respuesta.estado()).isEqualTo("INACTIVO");
    }

    @Test
    @DisplayName("404: courier-service no conoce al repartidor y se traduce a ResourceNotFoundException")
    void traduce404ARecursoNoEncontrado() {
        server.expect(requestTo(urlEsperada))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body("{}"));

        assertThatThrownBy(() -> client.consultarDisponibilidad(repartidorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(repartidorId.toString());
    }

    @Test
    @DisplayName("500: un error de courier-service se traduce a 503")
    void traduce5xxAServicioNoDisponible() {
        server.expect(requestTo(urlEsperada)).andRespond(withServerError());

        assertThatThrownBy(() -> client.consultarDisponibilidad(repartidorId))
                .isInstanceOf(CourierServiceUnavailableException.class);
    }

    @Test
    @DisplayName("Fallo de conexión: se traduce a 503")
    void traduceFalloDeConexionAServicioNoDisponible() {
        server.expect(requestTo(urlEsperada))
                .andRespond(request -> {
                    throw new IOException("connection refused");
                });

        assertThatThrownBy(() -> client.consultarDisponibilidad(repartidorId))
                .isInstanceOf(CourierServiceUnavailableException.class)
                .hasMessageContaining("No se pudo consultar courier-service");
    }

    @Test
    @DisplayName("Respuesta vacía: se traduce a 503 en lugar de devolver null")
    void traduceRespuestaVaciaAServicioNoDisponible() {
        server.expect(requestTo(urlEsperada))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.consultarDisponibilidad(repartidorId))
                .isInstanceOf(CourierServiceUnavailableException.class);
    }
}
