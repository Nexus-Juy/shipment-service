package ar.edu.unju.fi.logitrack.shipment.config;

import ar.edu.unju.fi.logitrack.common.security.JwtAccessDeniedHandler;
import ar.edu.unju.fi.logitrack.common.security.JwtAuthenticationEntryPoint;
import ar.edu.unju.fi.logitrack.common.security.ResourceServerSecurityConfig;
import ar.edu.unju.fi.logitrack.common.security.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Resource server de {@code shipment-service} (S2-06).
 * <p>
 * La validación del JWT, el converter de roles y las respuestas 401/403 son comunes y vienen de
 * {@link ResourceServerSecurityConfig}; acá solo se declara la matriz de autorización del servicio.
 * </p>
 */
@Configuration
@EnableWebSecurity
@Import(ResourceServerSecurityConfig.class)
public class SecurityConfig {

    /** Endpoints públicos: documentación, health para Docker y el dispatch de errores. */
    private static final String[] RUTAS_PUBLICAS = {
            "/actuator/health", "/actuator/health/**", "/actuator/info",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationEntryPoint entryPoint,
            JwtAccessDeniedHandler accessDeniedHandler,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(RUTAS_PUBLICAS).permitAll()
                // S2-06: el alta, la asignación y la cancelación son de ADMIN y CLIENTE.
                .requestMatchers(HttpMethod.POST, "/api/v1/envios").hasAnyRole(Roles.ADMIN, Roles.CLIENTE)
                .requestMatchers(HttpMethod.POST, "/api/v1/envios/*/cancelacion").hasAnyRole(Roles.ADMIN, Roles.CLIENTE)
                .requestMatchers(HttpMethod.PUT, "/api/v1/envios/*/repartidor").hasAnyRole(Roles.ADMIN, Roles.CLIENTE)
                // El cambio de estado lo hace el repartidor en ruta (o un ADMIN).
                .requestMatchers(HttpMethod.PATCH, "/api/v1/envios/*/estado").hasAnyRole(Roles.ADMIN, Roles.REPARTIDOR)
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .exceptionHandling(excepciones -> excepciones
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }
}
