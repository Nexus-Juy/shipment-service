package ar.edu.unju.fi.logitrack.shipment.client;

import ar.edu.unju.fi.logitrack.shipment.exception.CourierServiceUnavailableException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CourierErrorDecoderTest {

    private static final String ID = "3f6c1a52-8e0b-4d7a-9c11-0a1b2c3d4e01";

    private final CourierErrorDecoder decoder = new CourierErrorDecoder();

    @Test
    @DisplayName("404: repartidor inexistente, con su id en el mensaje")
    void traduce404ARecursoNoEncontrado() {
        Exception ex = decoder.decode("CourierFeignClient#disponibilidad", respuesta(404));

        assertThat(ex).isInstanceOf(ResourceNotFoundException.class);
        assertThat(ex.getMessage()).contains(ID);
    }

    @Test
    @DisplayName("500: error de courier-service, se traduce a 503")
    void traduce500AServicioNoDisponible() {
        Exception ex = decoder.decode("CourierFeignClient#disponibilidad", respuesta(500));

        assertThat(ex).isInstanceOf(CourierServiceUnavailableException.class);
        assertThat(ex.getMessage()).contains("500");
    }

    @Test
    @DisplayName("503 de courier-service: se traduce a 503")
    void traduce503AServicioNoDisponible() {
        Exception ex = decoder.decode("CourierFeignClient#disponibilidad", respuesta(503));

        assertThat(ex).isInstanceOf(CourierServiceUnavailableException.class);
    }

    @Test
    @DisplayName("401 entre servicios (token rechazado): tampoco rompe con un 500, es 503")
    void traduce401AServicioNoDisponible() {
        Exception ex = decoder.decode("CourierFeignClient#disponibilidad", respuesta(401));

        assertThat(ex).isInstanceOf(CourierServiceUnavailableException.class);
        assertThat(ex.getMessage()).contains("401");
    }

    private static Response respuesta(int estado) {
        Request peticion = Request.create(Request.HttpMethod.GET,
                "http://courier-service/api/v1/repartidores/" + ID + "/disponibilidad",
                Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
        return Response.builder()
                .status(estado)
                .reason("motivo")
                .request(peticion)
                .headers(Map.of())
                .build();
    }
}
