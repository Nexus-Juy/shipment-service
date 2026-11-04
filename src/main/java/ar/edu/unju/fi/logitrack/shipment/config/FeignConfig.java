package ar.edu.unju.fi.logitrack.shipment.config;

import ar.edu.unju.fi.logitrack.shipment.client.CourierFeignClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Activa Spring Cloud OpenFeign. Está separado de la clase principal para que los tests de capa
 * ({@code @WebMvcTest}, {@code @DataJpaTest}) no intenten crear los clientes Feign.
 */
@Configuration
@EnableFeignClients(basePackageClasses = CourierFeignClient.class)
public class FeignConfig {
}
