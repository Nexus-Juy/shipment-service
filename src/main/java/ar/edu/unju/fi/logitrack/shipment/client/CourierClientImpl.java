package ar.edu.unju.fi.logitrack.shipment.client;

import ar.edu.unju.fi.logitrack.shipment.dto.courier.DisponibilidadResponse;
import ar.edu.unju.fi.logitrack.shipment.exception.CourierServiceUnavailableException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Implementación HTTP de {@link CourierClient} basada en {@link RestClient}.
 * <p>
 * La URL base sale de {@code courier.service.url}, así el mismo código funciona contra
 * {@code localhost:8082} en desarrollo y contra {@code http://courier-service:8082} en Docker.
 * </p>
 */
@Component
public class CourierClientImpl implements CourierClient {

    private final RestClient restClient;

    public CourierClientImpl(RestClient.Builder restClientBuilder,
                             @Value("${courier.service.url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public DisponibilidadResponse consultarDisponibilidad(UUID repartidorId) {
        try {
            DisponibilidadResponse respuesta = restClient.get()
                    .uri("/api/v1/repartidores/{id}/disponibilidad", repartidorId)
                    .headers(headers -> {
                        // S2-06: courier-service exige JWT; se propaga el token del usuario que
                        // originó la operación para no perder la identidad en la llamada interna.
                        String token = tokenDelRequestActual();
                        if (token != null) {
                            headers.set(HttpHeaders.AUTHORIZATION, token);
                        }
                    })
                    .retrieve()
                    .body(DisponibilidadResponse.class);

            if (respuesta == null) {
                throw new CourierServiceUnavailableException(
                        "courier-service devolvió una respuesta vacía para el repartidor " + repartidorId, null);
            }
            return respuesta;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw ResourceNotFoundException.of("Repartidor", repartidorId);
            }
            throw new CourierServiceUnavailableException(
                    "courier-service respondió con estado " + ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            throw new CourierServiceUnavailableException(
                    "No se pudo consultar courier-service: " + ex.getMessage(), ex);
        }
    }

    /**
     * Token Bearer de la petición HTTP en curso, si la llamada se originó en un request.
     * Fuera de un request (por ejemplo en un test unitario) devuelve {@code null} y no se envía
     * el header.
     */
    private String tokenDelRequestActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        }
        return null;
    }
}
