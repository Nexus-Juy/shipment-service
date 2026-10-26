package ar.edu.unju.fi.logitrack.shipment.entity;

import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoEnvioTest {

    @DisplayName("Matriz completa de transiciones de la máquina de estados")
    @ParameterizedTest(name = "{0} → {1} permitido: {2}")
    @CsvSource({
            "PENDIENTE, PENDIENTE, false",
            "PENDIENTE, EN_RUTA,   true",
            "PENDIENTE, ENTREGADO, false",
            "PENDIENTE, CANCELADO, true",
            "EN_RUTA,   PENDIENTE, false",
            "EN_RUTA,   EN_RUTA,   false",
            "EN_RUTA,   ENTREGADO, true",
            "EN_RUTA,   CANCELADO, false",
            "ENTREGADO, PENDIENTE, false",
            "ENTREGADO, EN_RUTA,   false",
            "ENTREGADO, ENTREGADO, false",
            "ENTREGADO, CANCELADO, false",
            "CANCELADO, PENDIENTE, false",
            "CANCELADO, EN_RUTA,   false",
            "CANCELADO, ENTREGADO, false",
            "CANCELADO, CANCELADO, false"
    })
    void transiciones(EstadoEnvio origen, EstadoEnvio destino, boolean permitido) {
        assertThat(origen.puedeTransicionarA(destino)).isEqualTo(permitido);
    }

    @Test
    void entregadoYCanceladoSonEstadosFinales() {
        assertThat(EstadoEnvio.ENTREGADO.esFinal()).isTrue();
        assertThat(EstadoEnvio.CANCELADO.esFinal()).isTrue();
        assertThat(EstadoEnvio.PENDIENTE.esFinal()).isFalse();
        assertThat(EstadoEnvio.EN_RUTA.esFinal()).isFalse();
    }

    @Test
    void noSePuedeTransicionarANull() {
        assertThat(EstadoEnvio.PENDIENTE.puedeTransicionarA(null)).isFalse();
    }
}
