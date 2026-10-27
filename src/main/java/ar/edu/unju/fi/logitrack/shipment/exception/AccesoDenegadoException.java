package ar.edu.unju.fi.logitrack.shipment.exception;

/**
 * Se lanza cuando el usuario está autenticado pero no tiene permiso sobre el recurso pedido
 * (autorización por pertenencia, S2-07). {@code GlobalExceptionHandler} la traduce a un 403.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
