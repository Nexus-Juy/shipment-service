package ar.edu.unju.fi.logitrack.shipment.service.impl;

import ar.edu.unju.fi.logitrack.shipment.TestDataFactory;
import ar.edu.unju.fi.logitrack.shipment.client.CourierClient;
import ar.edu.unju.fi.logitrack.shipment.dto.asignacion.AsignacionResponse;
import ar.edu.unju.fi.logitrack.shipment.dto.courier.DisponibilidadResponse;
import ar.edu.unju.fi.logitrack.shipment.entity.Cliente;
import ar.edu.unju.fi.logitrack.shipment.entity.Envio;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;
import ar.edu.unju.fi.logitrack.shipment.exception.BusinessRuleException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import ar.edu.unju.fi.logitrack.shipment.mapper.AsignacionMapper;
import ar.edu.unju.fi.logitrack.shipment.repository.EnvioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de la asignación: reglas del envío y consulta remota de disponibilidad
 * a courier-service (mockeada).
 */
@ExtendWith(MockitoExtension.class)
class AsignacionServiceImplTest {

    @Mock
    private EnvioRepository envioRepository;

    @Mock
    private CourierClient courierClient;

    private AsignacionServiceImpl service;

    private final UUID envioId = UUID.randomUUID();
    private final UUID repartidorId = TestDataFactory.REPARTIDOR_ID;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        service = new AsignacionServiceImpl(envioRepository, courierClient, new AsignacionMapper());
        cliente = TestDataFactory.cliente();
    }

    private Envio crearEnvio(EstadoEnvio estado) {
        Envio envio = TestDataFactory.envioPendiente("LT-TEST-001", cliente);
        if (estado != EstadoEnvio.PENDIENTE) {
            envio.asignarRepartidor(repartidorId);
            if (estado == EstadoEnvio.CANCELADO) {
                envio.cancelar();
            } else if (estado == EstadoEnvio.EN_RUTA) {
                envio.cambiarEstado(EstadoEnvio.EN_RUTA);
            } else if (estado == EstadoEnvio.ENTREGADO) {
                envio.cambiarEstado(EstadoEnvio.EN_RUTA);
                envio.cambiarEstado(EstadoEnvio.ENTREGADO);
            }
        }
        ReflectionTestUtils.setField(envio, "id", envioId);
        return envio;
    }

    private DisponibilidadResponse disponibilidad(boolean disponible, String estado) {
        return new DisponibilidadResponse(repartidorId, estado, disponible);
    }

    @Test
    @DisplayName("asigna un repartidor ACTIVO a un envío PENDIENTE")
    void asignaRepartidorActivoAEnvioPendiente() {
        Envio envio = crearEnvio(EstadoEnvio.PENDIENTE);
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envio));
        when(courierClient.consultarDisponibilidad(repartidorId)).thenReturn(disponibilidad(true, "ACTIVO"));
        when(envioRepository.save(any(Envio.class))).thenAnswer(inv -> inv.getArgument(0));

        AsignacionResponse respuesta = service.asignarRepartidor(envioId, repartidorId);

        assertThat(envio.getRepartidorId()).isEqualTo(repartidorId);
        assertThat(respuesta.envioId()).isEqualTo(envioId);
        assertThat(respuesta.codigoSeguimiento()).isEqualTo("LT-TEST-001");
        assertThat(respuesta.estadoEnvio()).isEqualTo(EstadoEnvio.PENDIENTE);
        assertThat(respuesta.repartidorId()).isEqualTo(repartidorId);
        verify(envioRepository).save(envio);
    }

    @Test
    @DisplayName("permite reasignar el envío a otro repartidor mientras siga PENDIENTE")
    void reasignaEnvioPendienteAOtroRepartidor() {
        Envio envio = crearEnvio(EstadoEnvio.PENDIENTE);
        envio.asignarRepartidor(UUID.randomUUID());
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envio));
        when(courierClient.consultarDisponibilidad(repartidorId)).thenReturn(disponibilidad(true, "ACTIVO"));
        when(envioRepository.save(any(Envio.class))).thenAnswer(inv -> inv.getArgument(0));

        service.asignarRepartidor(envioId, repartidorId);

        assertThat(envio.getRepartidorId()).isEqualTo(repartidorId);
    }

    @Test
    @DisplayName("rechaza asignar un repartidor INACTIVO, según lo que informa courier-service")
    void rechazaRepartidorInactivo() {
        Envio envio = crearEnvio(EstadoEnvio.PENDIENTE);
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envio));
        when(courierClient.consultarDisponibilidad(repartidorId)).thenReturn(disponibilidad(false, "INACTIVO"));

        assertThatThrownBy(() -> service.asignarRepartidor(envioId, repartidorId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no está ACTIVO");

        assertThat(envio.getRepartidorId()).isNull();
        verify(envioRepository, never()).save(any());
    }

    @ParameterizedTest(name = "rechaza asignar si el envío está {0}")
    @EnumSource(value = EstadoEnvio.class, names = "PENDIENTE", mode = EnumSource.Mode.EXCLUDE)
    void rechazaEnvioQueNoEstaPendiente(EstadoEnvio estado) {
        Envio envio = crearEnvio(estado);
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envio));

        assertThatThrownBy(() -> service.asignarRepartidor(envioId, repartidorId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Solo se puede asignar repartidor a envíos PENDIENTES");

        // No se consulta courier-service si el envío no puede recibir un repartidor.
        verify(courierClient, never()).consultarDisponibilidad(any());
        verify(envioRepository, never()).save(any());
    }

    @Test
    @DisplayName("lanza 404 si el envío no existe")
    void envioInexistente() {
        when(envioRepository.findById(envioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.asignarRepartidor(envioId, repartidorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(envioId.toString());

        verify(courierClient, never()).consultarDisponibilidad(any());
    }

    @Test
    @DisplayName("propaga el 404 cuando courier-service no conoce al repartidor")
    void repartidorInexistente() {
        Envio envio = crearEnvio(EstadoEnvio.PENDIENTE);
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envio));
        when(courierClient.consultarDisponibilidad(repartidorId))
                .thenThrow(ResourceNotFoundException.of("Repartidor", repartidorId));

        assertThatThrownBy(() -> service.asignarRepartidor(envioId, repartidorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(repartidorId.toString());

        assertThat(envio.getRepartidorId()).isNull();
        verify(envioRepository, never()).save(any());
    }
}
