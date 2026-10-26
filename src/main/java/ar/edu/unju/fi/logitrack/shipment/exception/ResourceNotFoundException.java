package ar.edu.unju.fi.logitrack.shipment.exception;

/** Se traduce a HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String recurso, Object identificador) {
        return new ResourceNotFoundException("%s no encontrado: %s".formatted(recurso, identificador));
    }
}
