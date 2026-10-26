package ar.edu.unju.fi.logitrack.shipment.entity;

import ar.edu.unju.fi.logitrack.shipment.TestDataFactory;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;
import ar.edu.unju.fi.logitrack.shipment.exception.BusinessRuleException;
import ar.edu.unju.fi.logitrack.shipment.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static ar.edu.unju.fi.logitrack.shipment.TestDataFactory.cliente;
import static ar.edu.unju.fi.logitrack.shipment.TestDataFactory.envio;
import static ar.edu.unju.fi.logitrack.shipment.TestDataFactory.envioPendiente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvioTest {

    @Nested
    class Creacion {

        @Test
        void arrancaPendienteYRegistraHistorialInicial() {
            Envio envio = envioPendiente("LT-TEST000001", cliente());

            assertThat(envio.getEstado()).isEqualTo(EstadoEnvio.PENDIENTE);
            assertThat(envio.getHistorial()).hasSize(1);
            HistorialEstado inicial = envio.getHistorial().getFirst();
            assertThat(inicial.getEstadoAnterior()).isNull();
            assertThat(inicial.getEstadoNuevo()).isEqualTo(EstadoEnvio.PENDIENTE);
            assertThat(inicial.getFechaHora()).isNotNull();
        }

        @Test
        void rechazaPesoOMedidasNoPositivas() {
            assertThatThrownBy(() -> envio("LT-X", cliente(), "0", "10", "10", "10"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("peso");
            assertThatThrownBy(() -> envio("LT-X", cliente(), "1", "-5", "10", "10"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("alto");
        }
    }


    @Nested
    class CambioDeEstado {

        @Test
        void flujoCompletoQuedaAuditadoEnElHistorial() {
            Envio envio = envioPendiente("LT-F1", cliente());
            envio.asignarRepartidor(TestDataFactory.repartidorActivo());

            envio.cambiarEstado(EstadoEnvio.EN_RUTA);
            envio.cambiarEstado(EstadoEnvio.ENTREGADO);

            assertThat(envio.getEstado()).isEqualTo(EstadoEnvio.ENTREGADO);
            assertThat(envio.getHistorial())
                    .extracting(HistorialEstado::getEstadoAnterior, HistorialEstado::getEstadoNuevo)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(null, EstadoEnvio.PENDIENTE),
                            org.assertj.core.groups.Tuple.tuple(EstadoEnvio.PENDIENTE, EstadoEnvio.EN_RUTA),
                            org.assertj.core.groups.Tuple.tuple(EstadoEnvio.EN_RUTA, EstadoEnvio.ENTREGADO));
        }

        @Test
        void noPermiteSaltearEstados() {
            Envio envio = envioPendiente("LT-F2", cliente());

            assertThatThrownBy(() -> envio.cambiarEstado(EstadoEnvio.ENTREGADO))
                    .isInstanceOf(InvalidStateTransitionException.class);
            assertThat(envio.getEstado()).isEqualTo(EstadoEnvio.PENDIENTE);
            assertThat(envio.getHistorial()).hasSize(1);
        }

        @Test
        void noPermiteSalirARutaSinRepartidor() {
            Envio envio = envioPendiente("LT-F3", cliente());

            assertThatThrownBy(() -> envio.cambiarEstado(EstadoEnvio.EN_RUTA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("repartidor");
            assertThat(envio.getEstado()).isEqualTo(EstadoEnvio.PENDIENTE);
        }

        @Test
        void unEnvioEntregadoNoPuedeVolverAtras() {
            Envio envio = envioPendiente("LT-F4", cliente());
            envio.asignarRepartidor(TestDataFactory.repartidorActivo());
            envio.cambiarEstado(EstadoEnvio.EN_RUTA);
            envio.cambiarEstado(EstadoEnvio.ENTREGADO);

            assertThatThrownBy(() -> envio.cambiarEstado(EstadoEnvio.EN_RUTA))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }
    }

    @Nested
    class Cancelacion {

        @Test
        void seCancelaSiEstaPendiente() {
            Envio envio = envioPendiente("LT-C1", cliente());

            assertThat(envio.puedeCancelarse()).isTrue();
            envio.cancelar();

            assertThat(envio.getEstado()).isEqualTo(EstadoEnvio.CANCELADO);
            assertThat(envio.getHistorial()).hasSize(2);
        }

        @Test
        void noSeCancelaSiYaEstaEnRuta() {
            Envio envio = envioPendiente("LT-C2", cliente());
            envio.asignarRepartidor(TestDataFactory.repartidorActivo());
            envio.cambiarEstado(EstadoEnvio.EN_RUTA);

            assertThat(envio.puedeCancelarse()).isFalse();
            assertThatThrownBy(envio::cancelar).isInstanceOf(BusinessRuleException.class);
            assertThat(envio.getEstado()).isEqualTo(EstadoEnvio.EN_RUTA);
        }
    }

    @Nested
    class AsignacionDeRepartidor {

        @Test
        void rechazaRepartidorInactivo() {
            Envio envio = envioPendiente("LT-A1", cliente());

            assertThatThrownBy(() -> envio.asignarRepartidor(TestDataFactory.repartidorInactivo()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("ACTIVO");
            assertThat(envio.getRepartidor()).isNull();
        }

        @Test
        void rechazaAsignarSiElEnvioYaNoEstaPendiente() {
            Envio envio = envioPendiente("LT-A2", cliente());
            envio.cancelar();

            assertThatThrownBy(() -> envio.asignarRepartidor(TestDataFactory.repartidorActivo()))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }
}
