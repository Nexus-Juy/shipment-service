package ar.edu.unju.fi.logitrack.shipment.service.impl;

import ar.edu.unju.fi.logitrack.shipment.dto.repartidor.RepartidorRequest;
import ar.edu.unju.fi.logitrack.shipment.dto.repartidor.RepartidorResponse;
import ar.edu.unju.fi.logitrack.shipment.entity.Repartidor;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoRepartidor;
import ar.edu.unju.fi.logitrack.shipment.exception.BusinessRuleException;
import ar.edu.unju.fi.logitrack.shipment.exception.DuplicateResourceException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import ar.edu.unju.fi.logitrack.shipment.mapper.RepartidorMapper;
import ar.edu.unju.fi.logitrack.shipment.repository.EnvioRepository;
import ar.edu.unju.fi.logitrack.shipment.repository.RepartidorRepository;
import ar.edu.unju.fi.logitrack.shipment.service.RepartidorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RepartidorServiceImpl implements RepartidorService {

    private final RepartidorRepository repartidorRepository;
    private final EnvioRepository envioRepository;
    private final RepartidorMapper repartidorMapper;

    @Override
    @Transactional
    public RepartidorResponse registrar(RepartidorRequest request) {
        String email = request.email().trim();
        if (repartidorRepository.existsByEmailIgnoreCase(email)) {
            throw DuplicateResourceException.of("repartidor", "email", email);
        }
        Repartidor guardado = repartidorRepository.save(repartidorMapper.toEntity(request));
        return repartidorMapper.toResponse(guardado);
    }

    @Override
    public List<RepartidorResponse> listar(EstadoRepartidor estado) {
        List<Repartidor> repartidores = (estado == null)
                ? repartidorRepository.findAllByOrderByNombreAsc()
                : repartidorRepository.findByEstadoOrderByNombreAsc(estado);
        return repartidores.stream().map(repartidorMapper::toResponse).toList();
    }

    @Override
    public RepartidorResponse obtenerPorId(UUID id) {
        return repartidorMapper.toResponse(obtenerEntidad(id));
    }

    @Override
    @Transactional
    public RepartidorResponse actualizar(UUID id, RepartidorRequest request) {
        Repartidor repartidor = obtenerEntidad(id);
        String email = request.email().trim();
        if (repartidorRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw DuplicateResourceException.of("repartidor", "email", email);
        }
        repartidor.actualizarDatos(request.nombre().trim(), request.tipoVehiculo(), email, request.telefono().trim());
        return repartidorMapper.toResponse(repartidorRepository.save(repartidor));
    }

    @Override
    @Transactional
    public RepartidorResponse cambiarEstado(UUID id, EstadoRepartidor nuevoEstado) {
        Repartidor repartidor = obtenerEntidad(id);

        if (nuevoEstado == EstadoRepartidor.INACTIVO
                && envioRepository.existsByRepartidorIdAndEstado(id, EstadoEnvio.EN_RUTA)) {
            throw new BusinessRuleException(
                    "No se puede desactivar al repartidor: tiene envíos EN_RUTA pendientes de entrega");
        }

        repartidor.cambiarEstado(nuevoEstado);
        return repartidorMapper.toResponse(repartidorRepository.save(repartidor));
    }

    private Repartidor obtenerEntidad(UUID id) {
        return repartidorRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Repartidor", id));
    }
}
