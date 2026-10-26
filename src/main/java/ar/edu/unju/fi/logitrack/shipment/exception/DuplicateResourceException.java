package ar.edu.unju.fi.logitrack.shipment.exception;

/** Se intenta crear o modificar un recurso con un dato único que ya existe. Se traduce a HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public static DuplicateResourceException of(String recurso, String campo, Object valor) {
        return new DuplicateResourceException("Ya existe un %s con %s: %s".formatted(recurso, campo, valor));
    }
}
