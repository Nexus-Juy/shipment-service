package ar.edu.unju.fi.logitrack.shipment.repository;

import ar.edu.unju.fi.logitrack.shipment.TestDataFactory;
import ar.edu.unju.fi.logitrack.shipment.config.JpaAuditingConfig;
import ar.edu.unju.fi.logitrack.shipment.entity.Cliente;
import ar.edu.unju.fi.logitrack.shipment.entity.Envio;
import ar.edu.unju.fi.logitrack.shipment.entity.HistorialEstado;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Valida el mapeo JPA completo: embebidos, relaciones y el historial guardado por cascada. */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class EnvioRepositoryTest {

    @Autowired
    private EnvioRepository envioRepository;

    @Autowired
    private HistorialEstadoRepository historialEstadoRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void guardaEnvioConDireccionesYRecuperaPorCodigoDeSeguimiento() {
        Cliente cliente = entityManager.persist(TestDataFactory.cliente());
        envioRepository.save(TestDataFactory.envioPendiente("LT-REPO000001", cliente));
        entityManager.flush();
        entityManager.clear();

        Envio recuperado = envioRepository.findByCodigoSeguimiento("LT-REPO000001").orElseThrow();

        assertThat(recuperado.getId()).isNotNull();
        assertThat(recuperado.getOrigen()).isEqualTo(TestDataFactory.origen());
        assertThat(recuperado.getDestino()).isEqualTo(TestDataFactory.destino());
        assertThat(recuperado.getEstado()).isEqualTo(EstadoEnvio.PENDIENTE);
        assertThat(envioRepository.existsByCodigoSeguimiento("LT-REPO000001")).isTrue();
    }

    @Test
    void cadaCambioDeEstadoQuedaPersistidoEnElHistorial() {
        Cliente cliente = entityManager.persist(TestDataFactory.cliente());
        Envio envio = TestDataFactory.envioPendiente("LT-REPO000002", cliente);
        envio.asignarRepartidor(TestDataFactory.REPARTIDOR_ID);
        envio.cambiarEstado(EstadoEnvio.EN_RUTA);
        Envio guardado = envioRepository.saveAndFlush(envio);
        entityManager.clear();

        List<HistorialEstado> historial = historialEstadoRepository.findByEnvioIdOrderByFechaHoraDesc(guardado.getId());

        assertThat(historial)
                .extracting(HistorialEstado::getEstadoNuevo)
                .containsExactlyInAnyOrder(EstadoEnvio.PENDIENTE, EstadoEnvio.EN_RUTA);
        assertThat(historial).allSatisfy(h -> assertThat(h.getCreatedAt()).isNotNull());
    }

    @Test
    void findByEstado_filtraPorEstadoActual() {
        Cliente cliente = entityManager.persist(TestDataFactory.cliente());
        envioRepository.save(TestDataFactory.envioPendiente("LT-REPO000003", cliente));
        Envio cancelado = TestDataFactory.envioPendiente("LT-REPO000004", cliente);
        cancelado.cancelar();
        envioRepository.save(cancelado);
        entityManager.flush();

        assertThat(envioRepository.findByEstadoOrderByCreatedAtDesc(EstadoEnvio.CANCELADO))
                .extracting(Envio::getCodigoSeguimiento)
                .containsExactly("LT-REPO000004");
    }

    @Test
    void existsByRepartidorIdAndEstado_devuelveTrueSoloSiCoincideRepartidorYEstado() {
        Cliente cliente = entityManager.persist(TestDataFactory.cliente());
        Envio envio = TestDataFactory.envioPendiente("LT-REPO000005", cliente);
        envio.asignarRepartidor(TestDataFactory.REPARTIDOR_ID);
        envio.cambiarEstado(EstadoEnvio.EN_RUTA);
        envioRepository.saveAndFlush(envio);

        assertThat(envioRepository.existsByRepartidorIdAndEstado(TestDataFactory.REPARTIDOR_ID, EstadoEnvio.EN_RUTA)).isTrue();
        assertThat(envioRepository.existsByRepartidorIdAndEstado(TestDataFactory.REPARTIDOR_ID, EstadoEnvio.PENDIENTE)).isFalse();
        assertThat(envioRepository.existsByRepartidorIdAndEstado(UUID.randomUUID(), EstadoEnvio.EN_RUTA)).isFalse();
    }
}
