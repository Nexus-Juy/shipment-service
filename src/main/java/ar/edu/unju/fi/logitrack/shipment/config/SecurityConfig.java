package ar.edu.unju.fi.logitrack.shipment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Resource server de {@code shipment-service} (S2-06).
 * <p>
 * Valida la firma HS256 y la expiración de los JWT emitidos por {@code security-service}; no
 * gestiona usuarios ni emite tokens. Los roles llegan en el claim {@code roles} y se traducen a
 * autoridades {@code ROLE_*} para poder restringir las operaciones de escritura.
 * </p>
 */
@Configuration
@EnableWebSecurity
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
                .requestMatchers(HttpMethod.POST, "/api/v1/envios").hasAnyRole("ADMIN", "CLIENTE")
                .requestMatchers(HttpMethod.POST, "/api/v1/envios/*/cancelacion").hasAnyRole("ADMIN", "CLIENTE")
                .requestMatchers(HttpMethod.PUT, "/api/v1/envios/*/repartidor").hasAnyRole("ADMIN", "CLIENTE")
                // El cambio de estado lo hace el repartidor en ruta (o un ADMIN).
                .requestMatchers(HttpMethod.PATCH, "/api/v1/envios/*/estado").hasAnyRole("ADMIN", "REPARTIDOR")
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

    /**
     * Valida los JWT con el mismo secreto compartido que usa {@code security-service}.
     * En Docker llega por la variable de entorno {@code JWT_SECRET}.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        SecretKey clave = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(
                org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
    }

    /** Convierte el claim {@code roles: ["CLIENTE"]} en la autoridad {@code ROLE_CLIENTE}. */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("roles");
        autoridades.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(autoridades);
        return converter;
    }
}
