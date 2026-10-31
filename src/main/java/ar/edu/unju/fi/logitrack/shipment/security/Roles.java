package ar.edu.unju.fi.logitrack.shipment.security;

/**
 * Roles compartidos por los microservicios de LogiTrack.
 * <p>
 * Los valores sin prefijo se usan en {@code hasRole}/{@code hasAnyRole}; los que llevan
 * {@code ROLE_} se comparan contra las autoridades del {@code Authentication}.
 * </p>
 */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String CLIENTE = "CLIENTE";
    public static final String REPARTIDOR = "REPARTIDOR";

    public static final String ROLE_ADMIN = "ROLE_" + ADMIN;
    public static final String ROLE_CLIENTE = "ROLE_" + CLIENTE;
    public static final String ROLE_REPARTIDOR = "ROLE_" + REPARTIDOR;

    private Roles() {
    }
}
