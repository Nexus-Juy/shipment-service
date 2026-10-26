package ar.edu.unju.fi.logitrack.shipment.exception;

/** Se traduce a HTTP 503 cuando courier-service no responde o devuelve un error inesperado. */
public class CourierServiceUnavailableException extends RuntimeException {

    public CourierServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
