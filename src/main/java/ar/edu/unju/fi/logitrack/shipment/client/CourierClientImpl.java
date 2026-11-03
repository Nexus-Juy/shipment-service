package ar.edu.unju.fi.logitrack.shipment.client;

import ar.edu.unju.fi.logitrack.shipment.dto.courier.DisponibilidadResponse;
import ar.edu.unju.fi.logitrack.shipment.exception.CourierServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Implementación de {@link CourierClient} sobre {@link CourierFeignClient} (OpenFeign).
 * <p>
 * Ya no traduce errores: eso lo hacen {@link CourierErrorDecoder} (404 y 5xx) y
 * {@link CourierFeignFallbackFactory} (courier caído, timeout, sin instancias o circuito abierto). Acá solo
 * se toma el JWT del usuario que originó la operación y se le pasa al cliente, para no perder su identidad
 * en la llamada interna (S2-06).
 * </p>
 */
@Component
@RequiredArgsConstructor
public class CourierClientImpl implements CourierClient {

    private final CourierFeignClient courierFeignClient;

    @Override
    public DisponibilidadResponse consultarDisponibilidad(UUID repartidorId) {
        DisponibilidadResponse respuesta = courierFeignClient.disponibilidad(repartidorId, jwtDeLaPeticionActual());
        if (respuesta == null) {
            throw new CourierServiceUnavailableException(
                    "courier-service devolvió una respuesta vacía para el repartidor " + repartidorId, null);
        }
        return respuesta;
    }

    /** Header Authorization de la petición HTTP en curso, o {@code null} si no hay (por ejemplo, en un test unitario). */
    private static String jwtDeLaPeticionActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return atributos.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        }
        return null;
    }
}
