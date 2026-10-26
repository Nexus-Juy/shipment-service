package ar.edu.unju.fi.logitrack.shipment.entity.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Máquina de estados del envío:
 * <pre>
 *   PENDIENTE ──► EN_RUTA ──► ENTREGADO
 *       │
 *       └──────► CANCELADO
 * </pre>
 * Solo se puede cancelar mientras el envío está PENDIENTE.
 */
public enum EstadoEnvio {
    PENDIENTE,
    EN_RUTA,
    ENTREGADO,
    CANCELADO;

    public Set<EstadoEnvio> transicionesPermitidas() {
        return switch (this) {
            case PENDIENTE -> EnumSet.of(EN_RUTA, CANCELADO);
            case EN_RUTA -> EnumSet.of(ENTREGADO);
            case ENTREGADO, CANCELADO -> EnumSet.noneOf(EstadoEnvio.class);
        };
    }

    public boolean puedeTransicionarA(EstadoEnvio destino) {
        return destino != null && transicionesPermitidas().contains(destino);
    }

    public boolean esFinal() {
        return transicionesPermitidas().isEmpty();
    }
}
