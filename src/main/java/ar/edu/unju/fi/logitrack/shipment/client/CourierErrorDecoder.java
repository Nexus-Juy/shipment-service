package ar.edu.unju.fi.logitrack.shipment.client;

import ar.edu.unju.fi.logitrack.shipment.exception.CourierServiceUnavailableException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduce las respuestas de error de courier-service a excepciones de dominio (S3-04):
 * <ul>
 *   <li>{@code 404}: el repartidor no existe → {@link ResourceNotFoundException} (HTTP 404).</li>
 *   <li>cualquier otro error ({@code 5xx}, o un {@code 4xx} inesperado como un 401 entre servicios) →
 *       {@link CourierServiceUnavailableException} (HTTP 503).</li>
 * </ul>
 */
public class CourierErrorDecoder implements ErrorDecoder {

    private static final Pattern ID_EN_LA_RUTA = Pattern.compile("/repartidores/([^/?]+)");

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            return ResourceNotFoundException.of("Repartidor", idDeLaPeticion(response));
        }
        return new CourierServiceUnavailableException(
                "courier-service respondió con estado " + response.status(),
                FeignException.errorStatus(methodKey, response));
    }

    /** El decoder no recibe los argumentos: el id del repartidor se toma de la ruta que se pidió. */
    private static String idDeLaPeticion(Response response) {
        if (response.request() == null) {
            return "desconocido";
        }
        Matcher m = ID_EN_LA_RUTA.matcher(response.request().url());
        return m.find() ? m.group(1) : "desconocido";
    }
}
