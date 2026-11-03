package ar.edu.unju.fi.logitrack.shipment.client;

import ar.edu.unju.fi.logitrack.shipment.exception.CourierServiceUnavailableException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Fallback de {@link CourierFeignClient} (S3-04). Se ejecuta cuando la llamada a courier-service falla:
 * courier caído, timeout, sin instancias en Eureka, error {@code 5xx} o circuito abierto.
 * <p>
 * No inventa una respuesta: un repartidor "disponible" falso permitiría asignar envíos sin saber si el
 * repartidor realmente lo está. Responde con una excepción de dominio que el
 * {@code GlobalExceptionHandler} convierte en un {@code ProblemDetail} estructurado, así el servicio
 * sigue funcionando y solo falla la operación que necesita a courier-service.
 * </p>
 */
@Component
public class CourierFeignFallbackFactory implements FallbackFactory<CourierFeignClient> {

    @Override
    public CourierFeignClient create(Throwable causa) {
        return (repartidorId, authorization) -> {
            throw traducir(causa);
        };
    }

    static RuntimeException traducir(Throwable causa) {
        Throwable real = desenvolver(causa);

        // Respuestas de negocio que ya tradujo el ErrorDecoder: se dejan pasar tal cual.
        if (real instanceof ResourceNotFoundException || real instanceof CourierServiceUnavailableException) {
            return (RuntimeException) real;
        }
        if (real instanceof CallNotPermittedException) {
            return new CourierServiceUnavailableException(
                    "courier-service no disponible: circuito abierto, se reintentará en breve", real);
        }
        return new CourierServiceUnavailableException(
                "No se pudo consultar courier-service: " + real.getMessage(), real);
    }

    /** Resilience4j puede envolver la excepción original en CompletionException o ExecutionException. */
    private static Throwable desenvolver(Throwable t) {
        Throwable actual = t;
        while ((actual instanceof CompletionException || actual instanceof ExecutionException)
                && actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual;
    }
}
