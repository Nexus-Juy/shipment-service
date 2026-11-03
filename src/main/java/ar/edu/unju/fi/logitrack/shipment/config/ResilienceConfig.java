package ar.edu.unju.fi.logitrack.shipment.config;

import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit breaker de las llamadas Feign a courier-service (S3-04).
 * <ul>
 *   <li>Se abre cuando falla la mitad de las últimas 10 llamadas (con al menos 5 hechas) y queda abierto
 *       10 segundos: en ese tiempo responde de inmediato 503, sin llamar a courier-service. Después deja
 *       pasar 2 llamadas de prueba y, si salen bien, se cierra solo.</li>
 *   <li>Un 404 (repartidor inexistente) es una respuesta de negocio, no una falla: no cuenta para abrirlo.</li>
 *   <li>El límite de tiempo es de 6 s, apenas mayor que el {@code readTimeout} de Feign (5 s). Sin esto,
 *       Resilience4j cortaría la llamada al segundo (su valor por defecto).</li>
 * </ul>
 */
@Configuration
public class ResilienceConfig {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> circuitBreakerPorDefecto() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(2)
                        .ignoreExceptions(ResourceNotFoundException.class)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(6))
                        .build())
                .build());
    }
}
