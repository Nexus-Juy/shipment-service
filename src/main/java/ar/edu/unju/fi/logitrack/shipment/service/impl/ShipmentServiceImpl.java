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
import ar.edu.unju.fi.logitrack.shipment.exception.BusinessRuleException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import ar.edu.unju.fi.logitrack.shipment.mapper.EnvioMapper;
import ar.edu.unju.fi.logitrack.shipment.repository.ClienteRepository;
import ar.edu.unju.fi.logitrack.shipment.repository.EnvioRepository;
import ar.edu.unju.fi.logitrack.shipment.repository.HistorialEstadoRepository;
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
        return envioMapper.toResponse(buscarEnvio(id));
    }

    @Override
    @Transactional(readOnly = true)
    public EnvioResponse obtenerPorCodigoSeguimiento(String codigo) {
        Envio envio = envioRepository.findByCodigoSeguimiento(codigo)
            .orElseThrow(() -> ResourceNotFoundException.of("Envío con código", codigo));
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
        envio.cambiarEstado(request.estado());
        return envioMapper.toResponse(envioRepository.save(envio));
    }

    @Override
    @Transactional
    public EnvioResponse cancelarEnvio(UUID id) {
        Envio envio = buscarEnvio(id);
        envio.cancelar();
        return envioMapper.toResponse(envioRepository.save(envio));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialEstadoResponse> obtenerHistorial(UUID id) {
        if (!envioRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Envío", id);
        }
        return historialEstadoRepository.findByEnvioIdOrderByFechaHoraDesc(id).stream()
            .map(envioMapper::toHistorialResponse)
            .toList();
    }

    private List<Envio> buscarSegunFiltros(EstadoEnvio estado, UUID repartidorId) {
        if (repartidorId != null && estado != null) {
            return envioRepository.findByRepartidorIdAndEstadoOrderByCreatedAtDesc(repartidorId, estado);
        }
        if (repartidorId != null) {
            return envioRepository.findByRepartidorIdOrderByCreatedAtDesc(repartidorId);
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
     * enviados y, si no existe, lo registra.
     */
    private Cliente obtenerOCrearCliente(RegistrarEnvioRequest request) {
        if (request.clienteId() != null) {
            return clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", request.clienteId()));
        }
        DatosClienteRequest datos = request.cliente();
        if (datos == null) {
            throw new BusinessRuleException("Para registrar un envío hay que indicar el clienteId o los datos del cliente.");
        }
        String email = datos.email().trim();
        return clienteRepository.findByEmailIgnoreCase(email)
            .orElseGet(() -> clienteRepository.save(new Cliente(datos.nombre().trim(), email, datos.telefono())));
    }

    private String generarCodigoUnico() {
        String codigo;
        do {
            codigo = trackingCodeGenerator.generar();
        } while (envioRepository.existsByCodigoSeguimiento(codigo));
        return codigo;
    }
}
