package ar.edu.unju.fi.logitrack.shipment.service.impl;

import ar.edu.unju.fi.logitrack.shipment.dto.repartidor.AsignacionResponse;
import ar.edu.unju.fi.logitrack.shipment.entity.Envio;
import ar.edu.unju.fi.logitrack.shipment.entity.Repartidor;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import ar.edu.unju.fi.logitrack.shipment.mapper.AsignacionMapper;
import ar.edu.unju.fi.logitrack.shipment.repository.EnvioRepository;
import ar.edu.unju.fi.logitrack.shipment.repository.RepartidorRepository;
import ar.edu.unju.fi.logitrack.shipment.service.AsignacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AsignacionServiceImpl implements AsignacionService {

    private final EnvioRepository envioRepository;
    private final RepartidorRepository repartidorRepository;
    private final AsignacionMapper asignacionMapper;

    @Override
    @Transactional
    public AsignacionResponse asignarRepartidor(UUID envioId, UUID repartidorId) {
        Envio envio = envioRepository.findById(envioId)
                .orElseThrow(() -> ResourceNotFoundException.of("Envio", envioId));
        Repartidor repartidor = repartidorRepository.findById(repartidorId)
                .orElseThrow(() -> ResourceNotFoundException.of("Repartidor", repartidorId));

        envio.asignarRepartidor(repartidor);
        Envio guardado = envioRepository.save(envio);

        return asignacionMapper.toResponse(guardado);
    }
}
