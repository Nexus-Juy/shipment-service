package ar.edu.unju.fi.logitrack.shipment.exception;

/**
 * Violación de una regla de negocio (la petición está bien formada pero no se puede procesar).
 * Se traduce a HTTP 422 Unprocessable Entity.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
