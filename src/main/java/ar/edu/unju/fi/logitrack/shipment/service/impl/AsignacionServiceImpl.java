package ar.edu.unju.fi.logitrack.shipment.service.impl;

import ar.edu.unju.fi.logitrack.shipment.client.CourierClient;
import ar.edu.unju.fi.logitrack.shipment.dto.asignacion.AsignacionResponse;
import ar.edu.unju.fi.logitrack.shipment.dto.courier.DisponibilidadResponse;
import ar.edu.unju.fi.logitrack.shipment.entity.Envio;
import ar.edu.unju.fi.logitrack.shipment.exception.BusinessRuleException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import ar.edu.unju.fi.logitrack.shipment.mapper.AsignacionMapper;
import ar.edu.unju.fi.logitrack.shipment.repository.EnvioRepository;
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
    private final CourierClient courierClient;
    private final AsignacionMapper asignacionMapper;

    @Override
    @Transactional
    public AsignacionResponse asignarRepartidor(UUID envioId, UUID repartidorId) {
        Envio envio = envioRepository.findById(envioId)
                .orElseThrow(() -> ResourceNotFoundException.of("Envio", envioId));

        // Primero se valida el envío para no gastar una llamada remota en un pedido inválido.
        envio.validarAsignacionPosible();

        DisponibilidadResponse disponibilidad = courierClient.consultarDisponibilidad(repartidorId);
        if (!disponibilidad.disponible()) {
            throw new BusinessRuleException(
                    "El repartidor %s no está ACTIVO (estado: %s).".formatted(repartidorId, disponibilidad.estado()));
        }

        envio.asignarRepartidor(repartidorId);
        Envio guardado = envioRepository.save(envio);

        return asignacionMapper.toResponse(guardado);
    }
}
