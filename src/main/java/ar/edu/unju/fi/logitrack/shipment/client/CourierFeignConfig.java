package ar.edu.unju.fi.logitrack.shipment.client;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Configuración propia de {@link CourierFeignClient}.
 * <p>
 * No lleva {@code @Configuration}: Spring Cloud OpenFeign la aplica solo a este cliente. Si se
 * anotara, el component scan la volvería global y afectaría a cualquier otro cliente Feign.
 * </p>
 */
public class CourierFeignConfig {

    /**
     * S2-06: courier-service exige JWT. Se propaga el token del usuario que originó la operación
     * para no perder su identidad en la llamada interna. Fuera de un request HTTP (por ejemplo en un
     * test unitario) no hay token y no se envía el header.
     */
    @Bean
    public RequestInterceptor propagarJwt() {
        return plantilla -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
                String token = atributos.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (token != null) {
                    plantilla.header(HttpHeaders.AUTHORIZATION, token);
                }
            }
        };
    }
}
