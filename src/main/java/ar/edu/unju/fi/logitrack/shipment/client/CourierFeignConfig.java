package ar.edu.unju.fi.logitrack.shipment.client;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

/**
 * Configuración propia de {@link CourierFeignClient}.
 * <p>
 * No lleva {@code @Configuration}: Spring Cloud OpenFeign la aplica solo a este cliente. Si se
 * anotara, el component scan la volvería global y afectaría a cualquier otro cliente Feign.
 * </p>
 */
public class CourierFeignConfig {

    @Bean
    public ErrorDecoder courierErrorDecoder() {
        return new CourierErrorDecoder();
    }
}
