package ar.edu.unju.fi.logitrack.shipment.security;

import ar.edu.unju.fi.logitrack.shipment.TestDataFactory;
import ar.edu.unju.fi.logitrack.shipment.entity.Cliente;
import ar.edu.unju.fi.logitrack.shipment.entity.Envio;
import ar.edu.unju.fi.logitrack.shipment.exception.AccesoDenegadoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reglas de autorización por pertenencia (S2-07), probadas sobre el contexto de seguridad real
 * a partir de tokens firmados con el mismo formato que emite security-service.
 */
class AutorizacionEnvioTest {

    private static final UUID USUARIO_CLIENTE = UUID.randomUUID();
    private static final UUID OTRO_USUARIO = UUID.randomUUID();
    private static final UUID REPARTIDOR = TestDataFactory.REPARTIDOR_ID;
    private static final UUID OTRO_REPARTIDOR = UUID.fromString("3f6c1a52-8e0b-4d7a-9c11-0a1b2c3d4e02");

    private final UsuarioActual usuario = new UsuarioActual();
    private final AutorizacionEnvio autorizacion = new AutorizacionEnvio(usuario);

    private Envio envioDelCliente;
    private Envio envioAsignadoAlRepartidor;

    @BeforeEach
    void setUp() {
        Cliente cliente = TestDataFactory.cliente();
        cliente.vincularUsuario(USUARIO_CLIENTE);
        envioDelCliente = TestDataFactory.envioPendiente("LT-CLIENTE-01", cliente);

        Cliente otroCliente = TestDataFactory.cliente("otro@logitrack.test");
        otroCliente.vincularUsuario(OTRO_USUARIO);
        envioAsignadoAlRepartidor = TestDataFactory.envioPendiente("LT-REPARTIDOR-01", otroCliente);
        envioAsignadoAlRepartidor.asignarRepartidor(REPARTIDOR);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("CLIENTE")
    class ClienteTest {

        @BeforeEach
        void autenticar() {
            autenticarCon(JwtTestSupport.tokenDe(USUARIO_CLIENTE, "CLIENTE"));
        }

        @Test
        @DisplayName("accede a su propio envío")
        void accedeASuEnvio() {
            assertThatCode(() -> autorizacion.verificarLectura(envioDelCliente))
                .doesNotThrowAnyException();
            assertThatCode(() -> autorizacion.verificarCancelacion(envioDelCliente))
                .doesNotThrowAnyException();
            assertThatCode(() -> autorizacion.verificarAsignacion(envioDelCliente))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("recibe 403 con el envío de otro cliente")
        void noAccedeAEnvioAjeno() {
            assertThatThrownBy(() -> autorizacion.verificarLectura(envioAsignadoAlRepartidor))
                .isInstanceOf(AccesoDenegadoException.class);
            assertThatThrownBy(() -> autorizacion.verificarCancelacion(envioAsignadoAlRepartidor))
                .isInstanceOf(AccesoDenegadoException.class);
            assertThatThrownBy(() -> autorizacion.verificarAsignacion(envioAsignadoAlRepartidor))
                .isInstanceOf(AccesoDenegadoException.class);
        }

        @Test
        @DisplayName("no puede cambiar el estado de un envío")
        void noCambiaEstado() {
            assertThatThrownBy(() -> autorizacion.verificarCambioDeEstado(envioDelCliente))
                .isInstanceOf(AccesoDenegadoException.class);
        }
    }

    @Nested
    @DisplayName("REPARTIDOR")
    class RepartidorTest {

        @BeforeEach
        void autenticar() {
            autenticarCon(JwtTestSupport.tokenDeRepartidor(REPARTIDOR, "REPARTIDOR"));
        }

        @Test
        @DisplayName("accede y actualiza los envíos que tiene asignados")
        void accedeASusEnvios() {
            assertThatCode(() -> autorizacion.verificarLectura(envioAsignadoAlRepartidor))
                .doesNotThrowAnyException();
            assertThatCode(() -> autorizacion.verificarCambioDeEstado(envioAsignadoAlRepartidor))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("recibe 403 con un envío que no tiene asignado")
        void noAccedeAEnvioNoAsignado() {
            assertThatThrownBy(() -> autorizacion.verificarLectura(envioDelCliente))
                .isInstanceOf(AccesoDenegadoException.class);
            assertThatThrownBy(() -> autorizacion.verificarCambioDeEstado(envioDelCliente))
                .isInstanceOf(AccesoDenegadoException.class);
        }

        @Test
        @DisplayName("no puede cancelar ni asignar repartidores")
        void noCancelaNiAsigna() {
            assertThatThrownBy(() -> autorizacion.verificarCancelacion(envioAsignadoAlRepartidor))
                .isInstanceOf(AccesoDenegadoException.class);
            assertThatThrownBy(() -> autorizacion.verificarAsignacion(envioAsignadoAlRepartidor))
                .isInstanceOf(AccesoDenegadoException.class);
        }

        @Test
        @DisplayName("solo puede filtrar el listado por su propio repartidorId")
        void filtraSoloSuPropioListado() {
            assertThat(autorizacion.repartidorIdParaListado(null)).isEqualTo(REPARTIDOR);
            assertThat(autorizacion.repartidorIdParaListado(REPARTIDOR)).isEqualTo(REPARTIDOR);
            assertThatThrownBy(() -> autorizacion.repartidorIdParaListado(OTRO_REPARTIDOR))
                .isInstanceOf(AccesoDenegadoException.class);
        }
    }

    @Nested
    @DisplayName("ADMIN")
    class AdminTest {

        @BeforeEach
        void autenticar() {
            autenticarCon(JwtTestSupport.tokenDe(UUID.randomUUID(), "ADMIN"));
        }

        @Test
        @DisplayName("mantiene acceso total")
        void accedeATodo() {
            List<Envio> envios = List.of(envioDelCliente, envioAsignadoAlRepartidor);
            envios.forEach(envio -> {
                assertThatCode(() -> autorizacion.verificarLectura(envio)).doesNotThrowAnyException();
                assertThatCode(() -> autorizacion.verificarCancelacion(envio)).doesNotThrowAnyException();
                assertThatCode(() -> autorizacion.verificarCambioDeEstado(envio)).doesNotThrowAnyException();
                assertThatCode(() -> autorizacion.verificarAsignacion(envio)).doesNotThrowAnyException();
            });
        }

        @Test
        @DisplayName("puede filtrar por cualquier repartidor")
        void filtraPorCualquierRepartidor() {
            assertThat(autorizacion.repartidorIdParaListado(OTRO_REPARTIDOR)).isEqualTo(OTRO_REPARTIDOR);
        }
    }

    @Test
    @DisplayName("Sin autenticación no se puede resolver el usuario ni listar")
    void sinAutenticacionNoHayAcceso() {
        assertThat(usuario.usuarioId()).isNull();
        assertThat(usuario.repartidorId()).isNull();
        assertThatThrownBy(() -> autorizacion.usuarioIdDelCliente())
            .isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> autorizacion.verificarLectura(envioDelCliente))
            .isInstanceOf(AccesoDenegadoException.class);
    }

    private void autenticarCon(String token) {
        Jwt jwt = JwtTestSupport.decodificar(token);
        List<GrantedAuthority> autoridades = jwt.getClaimAsStringList("roles").stream()
            .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + rol))
            .toList();
        SecurityContextHolder.getContext()
            .setAuthentication(new JwtAuthenticationToken(jwt, autoridades));
    }
}
