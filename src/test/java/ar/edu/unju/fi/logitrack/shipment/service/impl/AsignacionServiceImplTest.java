package ar.edu.unju.fi.logitrack.shipment.service.impl;

import ar.edu.unju.fi.logitrack.shipment.TestDataFactory;
import ar.edu.unju.fi.logitrack.shipment.dto.repartidor.AsignacionResponse;
import ar.edu.unju.fi.logitrack.shipment.entity.Cliente;
import ar.edu.unju.fi.logitrack.shipment.entity.Envio;
import ar.edu.unju.fi.logitrack.shipment.entity.Repartidor;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.TipoVehiculo;
import ar.edu.unju.fi.logitrack.shipment.exception.BusinessRuleException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import ar.edu.unju.fi.logitrack.shipment.mapper.AsignacionMapper;
import ar.edu.unju.fi.logitrack.shipment.mapper.RepartidorMapper;
import ar.edu.unju.fi.logitrack.shipment.repository.EnvioRepository;
import ar.edu.unju.fi.logitrack.shipment.repository.RepartidorRepository;
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

/** Tests unitarios de la asignación de repartidores: cubren todas las reglas de negocio. */
@ExtendWith(MockitoExtension.class)
class AsignacionServiceImplTest {

    @Mock
    private EnvioRepository envioRepository;

    @Mock
    private RepartidorRepository repartidorRepository;

    private AsignacionServiceImpl service;

    private final UUID envioId = UUID.randomUUID();
    private final UUID repartidorId = UUID.randomUUID();
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        service = new AsignacionServiceImpl(
                envioRepository, repartidorRepository, new AsignacionMapper(new RepartidorMapper()));
        cliente = TestDataFactory.cliente();
    }

    private Envio crearEnvio(EstadoEnvio estado) {
        Envio envio = TestDataFactory.envioPendiente("LT-TEST-001", cliente);
        if (estado != EstadoEnvio.PENDIENTE) {
            envio.asignarRepartidor(TestDataFactory.repartidorActivo());
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

    private Repartidor crearRepartidorActivo() {
        Repartidor repartidor = TestDataFactory.repartidorActivo();
        ReflectionTestUtils.setField(repartidor, "id", repartidorId);
        return repartidor;
    }

    private Repartidor crearRepartidorInactivo() {
        Repartidor repartidor = TestDataFactory.repartidorInactivo();
        ReflectionTestUtils.setField(repartidor, "id", repartidorId);
        return repartidor;
    }

    @Test
    @DisplayName("asigna un repartidor ACTIVO a un envío PENDIENTE")
    void asignaRepartidorActivoAEnvioPendiente() {
        Envio envio = crearEnvio(EstadoEnvio.PENDIENTE);
        Repartidor repartidor = crearRepartidorActivo();
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envio));
        when(repartidorRepository.findById(repartidorId)).thenReturn(Optional.of(repartidor));
        when(envioRepository.save(any(Envio.class))).thenAnswer(inv -> inv.getArgument(0));

        AsignacionResponse respuesta = service.asignarRepartidor(envioId, repartidorId);

        assertThat(envio.getRepartidor()).isSameAs(repartidor);
        assertThat(respuesta.envioId()).isEqualTo(envioId);
        assertThat(respuesta.codigoSeguimiento()).isEqualTo("LT-TEST-001");
        assertThat(respuesta.estadoEnvio()).isEqualTo(EstadoEnvio.PENDIENTE);
        assertThat(respuesta.repartidor().id()).isEqualTo(repartidorId);
        verify(envioRepository).save(envio);
    }

    @Test
    @DisplayName("permite reasignar el envío a otro repartidor mientras siga PENDIENTE")
    void reasignaEnvioPendienteAOtroRepartidor() {
        Envio envio = crearEnvio(EstadoEnvio.PENDIENTE);
        Repartidor repartidorAnterior = new Repartidor("Anterior", TipoVehiculo.AUTO, "anterior@logitrack.test", "+54 388 400-3000");
        envio.asignarRepartidor(repartidorAnterior);

        Repartidor nuevo = crearRepartidorActivo();
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envio));
        when(repartidorRepository.findById(repartidorId)).thenReturn(Optional.of(nuevo));
        when(envioRepository.save(any(Envio.class))).thenAnswer(inv -> inv.getArgument(0));

        service.asignarRepartidor(envioId, repartidorId);

        assertThat(envio.getRepartidor()).isSameAs(nuevo);
    }

    @Test
    @DisplayName("rechaza asignar un repartidor INACTIVO")
    void rechazaRepartidorInactivo() {
        Envio envio = crearEnvio(EstadoEnvio.PENDIENTE);
        Repartidor repartidor = crearRepartidorInactivo();
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envio));
        when(repartidorRepository.findById(repartidorId)).thenReturn(Optional.of(repartidor));

        assertThatThrownBy(() -> service.asignarRepartidor(envioId, repartidorId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no está ACTIVO");

        assertThat(envio.getRepartidor()).isNull();
        verify(envioRepository, never()).save(any());
    }

    @ParameterizedTest(name = "rechaza asignar si el envío está {0}")
    @EnumSource(value = EstadoEnvio.class, names = "PENDIENTE", mode = EnumSource.Mode.EXCLUDE)
    void rechazaEnvioQueNoEstaPendiente(EstadoEnvio estado) {
        Envio envio = crearEnvio(estado);
        Repartidor repartidor = crearRepartidorActivo();
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envio));
        when(repartidorRepository.findById(repartidorId)).thenReturn(Optional.of(repartidor));

        assertThatThrownBy(() -> service.asignarRepartidor(envioId, repartidorId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Solo se puede asignar repartidor a envíos PENDIENTES");

        verify(envioRepository, never()).save(any());
    }

    @Test
    @DisplayName("lanza 404 si el envío no existe")
    void envioInexistente() {
        when(envioRepository.findById(envioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.asignarRepartidor(envioId, repartidorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(envioId.toString());

        verify(repartidorRepository, never()).findById(any());
    }

    @Test
    @DisplayName("lanza 404 si el repartidor no existe")
    void repartidorInexistente() {
        Envio envio = crearEnvio(EstadoEnvio.PENDIENTE);
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envio));
        when(repartidorRepository.findById(repartidorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.asignarRepartidor(envioId, repartidorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(repartidorId.toString());

        verify(envioRepository, never()).save(any());
    }
}
