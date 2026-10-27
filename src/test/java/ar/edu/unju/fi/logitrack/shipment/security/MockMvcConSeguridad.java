package ar.edu.unju.fi.logitrack.shipment.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

/**
 * Spring Boot 4 ya no aplica {@code springSecurity()} a MockMvc de forma automática, así que los
 * tests que usan {@code @WithMockUser} necesitan registrar este customizer para que el usuario de
 * prueba llegue al filtro de seguridad.
 */
@TestConfiguration
public class MockMvcConSeguridad {

    @Bean
    MockMvcBuilderCustomizer securityMockMvcConfigurer() {
        return builder -> builder.apply(SecurityMockMvcConfigurers.springSecurity());
    }
}
