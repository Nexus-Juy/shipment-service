package ar.edu.unju.fi.logitrack.shipment.service.impl;

import ar.edu.unju.fi.logitrack.shipment.dto.envio.CambiarEstadoRequest;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.DatosClienteRequest;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.EnvioResponse;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.HistorialEstadoResponse;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.RegistrarEnvioRequest;
import ar.edu.unju.fi.logitrack.shipment.entity.Cliente;
import ar.edu.unju.fi.logitrack.shipment.entity.Direccion;
import ar.edu.unju.fi.logitrack.shipment.entity.Envio;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;
import ar.edu.unju.fi.logitrack.shipment.exception.AccesoDenegadoException;
import ar.edu.unju.fi.logitrack.shipment.exception.BusinessRuleException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import ar.edu.unju.fi.logitrack.shipment.mapper.EnvioMapper;
import ar.edu.unju.fi.logitrack.shipment.repository.ClienteRepository;
import ar.edu.unju.fi.logitrack.shipment.repository.EnvioRepository;
import ar.edu.unju.fi.logitrack.shipment.repository.HistorialEstadoRepository;
import ar.edu.unju.fi.logitrack.shipment.security.AutorizacionEnvio;
import ar.edu.unju.fi.logitrack.shipment.security.UsuarioActual;
import ar.edu.unju.fi.logitrack.shipment.service.ShipmentService;
import ar.edu.unju.fi.logitrack.shipment.service.TrackingCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private final EnvioRepository envioRepository;
    private final ClienteRepository clienteRepository;
    private final HistorialEstadoRepository historialEstadoRepository;
    private final EnvioMapper envioMapper;
    private final TrackingCodeGenerator trackingCodeGenerator;
    private final AutorizacionEnvio autorizacion;
    private final UsuarioActual usuarioActual;

    @Override
    @Transactional
    public EnvioResponse registrarEnvio(RegistrarEnvioRequest request) {
        Envio envio = new Envio(
            generarCodigoUnico(),
            obtenerOCrearCliente(request),
            new Direccion(request.origenCalle(), request.origenNumero(), request.origenCiudad(), request.origenCodigoPostal()),
            new Direccion(request.destinoCalle(), request.destinoNumero(), request.destinoCiudad(), request.destinoCodigoPostal()),
            request.peso(),
            request.alto(),
            request.ancho(),
            request.largo()
        );
        return envioMapper.toResponse(envioRepository.save(envio));
    }

    @Override
    @Transactional(readOnly = true)
    public EnvioResponse obtenerPorId(UUID id) {
        Envio envio = buscarEnvio(id);
        autorizacion.verificarLectura(envio);
        return envioMapper.toResponse(envio);
    }

    @Override
    @Transactional(readOnly = true)
    public EnvioResponse obtenerPorCodigoSeguimiento(String codigo) {
        Envio envio = envioRepository.findByCodigoSeguimiento(codigo)
            .orElseThrow(() -> ResourceNotFoundException.of("Envío con código", codigo));
        autorizacion.verificarLectura(envio);
        return envioMapper.toResponse(envio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvioResponse> listar(EstadoEnvio estado, UUID repartidorId) {
        return buscarSegunFiltros(estado, repartidorId).stream().map(envioMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public EnvioResponse cambiarEstado(UUID id, CambiarEstadoRequest request) {
        Envio envio = buscarEnvio(id);
        autorizacion.verificarCambioDeEstado(envio);
        envio.cambiarEstado(request.estado());
        return envioMapper.toResponse(envioRepository.save(envio));
    }

    @Override
    @Transactional
    public EnvioResponse cancelarEnvio(UUID id) {
        Envio envio = buscarEnvio(id);
        autorizacion.verificarCancelacion(envio);
        envio.cancelar();
        return envioMapper.toResponse(envioRepository.save(envio));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialEstadoResponse> obtenerHistorial(UUID id) {
        Envio envio = buscarEnvio(id);
        autorizacion.verificarLectura(envio);
        return historialEstadoRepository.findByEnvioIdOrderByFechaHoraDesc(id).stream()
            .map(envioMapper::toHistorialResponse)
            .toList();
    }

    /**
     * S2-07: el listado se recorta según quién consulta.
     * <ul>
     *   <li>{@code ADMIN}: todos los envíos, con los filtros pedidos.</li>
     *   <li>{@code CLIENTE}: solo los de su cliente; el filtro por repartidor se aplica sobre esos.</li>
     *   <li>{@code REPARTIDOR}: solo los asignados a él; pedir otro repartidor es 403.</li>
     * </ul>
     */
    private List<Envio> buscarSegunFiltros(EstadoEnvio estado, UUID repartidorId) {
        UUID filtro = autorizacion.repartidorIdParaListado(repartidorId);

        if (usuarioActual.esCliente()) {
            UUID usuarioId = autorizacion.usuarioIdDelCliente();
            List<Envio> propios = estado != null
                ? envioRepository.findByClienteUsuarioIdAndEstadoOrderByCreatedAtDesc(usuarioId, estado)
                : envioRepository.findByClienteUsuarioIdOrderByCreatedAtDesc(usuarioId);
            if (filtro == null) {
                return propios;
            }
            return propios.stream().filter(envio -> filtro.equals(envio.getRepartidorId())).toList();
        }

        if (filtro != null && estado != null) {
            return envioRepository.findByRepartidorIdAndEstadoOrderByCreatedAtDesc(filtro, estado);
        }
        if (filtro != null) {
            return envioRepository.findByRepartidorIdOrderByCreatedAtDesc(filtro);
        }
        if (estado != null) {
            return envioRepository.findByEstadoOrderByCreatedAtDesc(estado);
        }
        return envioRepository.findAll();
    }

    private Envio buscarEnvio(UUID id) {
        return envioRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Envío", id));
    }

    /**
     * Usa el cliente indicado por {@code clienteId}; si no viene, lo busca por email entre los datos
     * enviados y, si no existe, lo registra. El alta se guarda una sola vez, ya vinculada al usuario.
     * <p>
     * S2-07: si quien registra es un {@code CLIENTE}, el cliente queda vinculado a su usuario
     * ({@code sub} del token) para poder verificar después la pertenencia de los envíos.
     * </p>
     */
    private Cliente obtenerOCrearCliente(RegistrarEnvioRequest request) {
        UUID usuarioId = usuarioActual.esCliente() ? autorizacion.usuarioIdDelCliente() : null;

        if (request.clienteId() != null) {
            Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", request.clienteId()));
            vincularAlUsuario(cliente, usuarioId);
            return cliente;
        }

        DatosClienteRequest datos = request.cliente();
        if (datos == null) {
            throw new BusinessRuleException("Para registrar un envío hay que indicar el clienteId o los datos del cliente.");
        }
        String email = datos.email().trim();
        return clienteRepository.findByEmailIgnoreCase(email)
            .map(cliente -> {
                vincularAlUsuario(cliente, usuarioId);
                return cliente;
            })
            .orElseGet(() -> {
                Cliente nuevo = new Cliente(datos.nombre().trim(), email, datos.telefono());
                vincularAlUsuario(nuevo, usuarioId);
                return clienteRepository.save(nuevo);
            });
    }

    /** Un CLIENTE solo puede operar con clientes propios o todavía sin dueño. */
    private void vincularAlUsuario(Cliente cliente, UUID usuarioId) {
        if (usuarioId == null) {
            return;
        }
        if (cliente.getUsuarioId() == null) {
            cliente.vincularUsuario(usuarioId);
            return;
        }
        if (!cliente.esDeUsuario(usuarioId)) {
            throw new AccesoDenegadoException(
                "El cliente indicado está asociado a otro usuario y no puede registrar envíos a su nombre.");
        }
    }

    private String generarCodigoUnico() {
        String codigo;
        do {
            codigo = trackingCodeGenerator.generar();
        } while (envioRepository.existsByCodigoSeguimiento(codigo));
        return codigo;
    }
}
