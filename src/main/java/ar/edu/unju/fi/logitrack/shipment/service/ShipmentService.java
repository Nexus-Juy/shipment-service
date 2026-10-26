package ar.edu.unju.fi.logitrack.shipment.service;

import ar.edu.unju.fi.logitrack.shipment.dto.envio.CambiarEstadoRequest;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.EnvioResponse;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.HistorialEstadoResponse;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.RegistrarEnvioRequest;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;

import java.util.List;
import java.util.UUID;

/**
 * Contrato del ciclo de vida de los envíos: registro, consulta, cambios de estado y auditoría.
 */
public interface ShipmentService {

    EnvioResponse registrarEnvio(RegistrarEnvioRequest request);

    EnvioResponse obtenerPorId(UUID id);

    EnvioResponse obtenerPorCodigoSeguimiento(String codigo);

    /**
     * Lista envíos, del más nuevo al más viejo. Los filtros son opcionales y se pueden combinar:
     * {@code estado} (estado actual) y {@code repartidorId} (envíos asignados a ese repartidor).
     * Sin filtros devuelve todos.
     */
    List<EnvioResponse> listar(EstadoEnvio estado, UUID repartidorId);

    EnvioResponse cambiarEstado(UUID id, CambiarEstadoRequest request);

    /** Cancela el envío; solo es válido mientras está PENDIENTE. */
    EnvioResponse cancelarEnvio(UUID id);

    /** Historial de auditoría del envío, del cambio más reciente al más antiguo. */
    List<HistorialEstadoResponse> obtenerHistorial(UUID id);
}
