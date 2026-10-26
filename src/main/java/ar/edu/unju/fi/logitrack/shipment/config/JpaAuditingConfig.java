package ar.edu.unju.fi.logitrack.shipment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita la auditoría de Spring Data (createdAt / updatedAt en {@code BaseEntity}).
 * Está en una clase aparte (y no en la clase main) para que los tests @WebMvcTest
 * no intenten levantar JPA.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing
public class JpaAuditingConfig {
}
