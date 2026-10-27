package ar.edu.unju.fi.logitrack.shipment.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones de la aplicación a respuestas estandarizadas RFC 7807 (application/problem+json).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja excepciones cuando no se encuentra un recurso solicitado (HTTP 404).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    /**
     * Autorización por pertenencia (S2-07): el usuario está autenticado pero el recurso es de otro
     * (HTTP 403).
     */
    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ProblemDetail> handleAccesoDenegado(AccesoDenegadoException ex) {
        return build(HttpStatus.FORBIDDEN, "Acceso denegado", ex.getMessage());
    }

    /**
     * Maneja violaciones a reglas de negocio de la aplicación (HTTP 422).
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRule(BusinessRuleException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio violada", ex.getMessage());
    }

    /**
     * Maneja errores de restricciones de la base de datos como claves duplicadas (HTTP 409).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos", ex);
        return build(HttpStatus.CONFLICT, "Conflicto de datos",
            "La operación viola una restricción de la base de datos (posible dato duplicado).");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
        log.warn("Conflicto de concurrencia optimista", ex);
        return build(HttpStatus.CONFLICT, "Conflicto de concurrencia",
                "El recurso fue modificado concurrentemente por otra operación. Intente nuevamente.");
    }

    /**
     * Maneja la caída o el error de courier-service al consultar un repartidor (HTTP 503).
     */
    @ExceptionHandler(CourierServiceUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleCourierUnavailable(CourierServiceUnavailableException ex) {
        log.warn("courier-service no disponible: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible",
            "No se pudo consultar el microservicio de repartidores. Intente nuevamente más tarde.");
    }

    /**
     * Captura errores de validación de DTOs (@Valid) devolviendo el detalle por campo (HTTP 400).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ProblemDetail body = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "La solicitud contiene campos inválidos o incompletos."
        );
        body.setTitle("Datos inválidos");
        body.setProperty("timestamp", Instant.now());
        body.setProperty("errors", errores);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Captura errores de conversión de parámetros de ruta (ej. UUID mal formado) y responde HTTP 400.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = String.format("El parámetro '%s' no tiene un formato válido.", ex.getName());
        return build(HttpStatus.BAD_REQUEST, "Parámetro inválido", detail);
    }

    /**
     * Captura errores de deserialización JSON (ej. valores de Enum inválidos como 'AVION') y responde HTTP 400.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Cuerpo de solicitud inválido",
            "El cuerpo de la petición contiene un formato JSON inválido o un valor no permitido para un enumerado.");
    }

    /**
     * Captura cualquier otra excepción no controlada en el sistema (HTTP 500).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
            "Ocurrió un error inesperado. Intente nuevamente más tarde.");
    }

    /**
     * Método auxiliar para construir respuestas ProblemDetail estandarizadas.
     */
    private ResponseEntity<ProblemDetail> build(HttpStatus status, String title, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        body.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(status).body(body);
    }
}
