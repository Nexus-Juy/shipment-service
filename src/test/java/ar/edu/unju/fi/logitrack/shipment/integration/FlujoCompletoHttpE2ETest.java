package ar.edu.unju.fi.logitrack.shipment.integration;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba end-to-end real de S2-11: {@code registrar envío → asignar repartidor → cambiar estado}.
 * <p>
 * A diferencia de {@link GestionEnviosIntegrationTest} (que reemplaza {@code CourierClient} por un
 * mock), acá se levanta un <strong>courier-service falso sobre HTTP real</strong> con
 * {@link HttpServer} y se apunta {@code courier.service.url} a su puerto efímero. Así se ejercita
 * el camino completo: controller → service → {@code RestClient} → socket TCP → servicio externo,
 * incluyendo la serialización JSON de ida y vuelta.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class FlujoCompletoHttpE2ETest {

    private static final String ENVIOS = "/api/v1/envios";

    /** UUIDs de repartidores que conoce el courier falso (ver db/init/02-couriers.sql). */
    private static final UUID REPARTIDOR_ACTIVO =
            UUID.fromString("3f6c1a52-8e0b-4d7a-9c11-0a1b2c3d4e01");
    private static final UUID REPARTIDOR_INACTIVO =
            UUID.fromString("3f6c1a52-8e0b-4d7a-9c11-0a1b2c3d4e02");
    private static final UUID REPARTIDOR_DESCONOCIDO =
            UUID.fromString("3f6c1a52-8e0b-4d7a-9c11-0a1b2c3d4e99");

    private static final Pattern RUTA_DISPONIBILIDAD =
            Pattern.compile("^/api/v1/repartidores/([0-9a-fA-F-]{36})/disponibilidad$");

    private static final String BODY_ENVIO = """
        {
          "origenCalle": "Belgrano", "origenNumero": "450",
          "origenCiudad": "San Salvador de Jujuy", "origenCodigoPostal": "4600",
          "destinoCalle": "Av. Fascio", "destinoNumero": "1200",
          "destinoCiudad": "San Salvador de Jujuy", "destinoCodigoPostal": "4600",
          "peso": 2.5, "ancho": 30, "alto": 20, "largo": 10,
          "cliente": {"nombre": "Tienda Norte SRL", "email": "ventas@tiendanorte.com"}
        }
        """;

    /** Peticiones que recibió el courier falso, para comprobar que hubo tráfico HTTP de verdad. */
    private static final List<String> PETICIONES = new CopyOnWriteArrayList<>();

    private static final HttpServer COURIER_FALSO = iniciarCourierFalso();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void apuntarAlCourierFalso(DynamicPropertyRegistry registry) {
        registry.add("courier.service.url",
                () -> "http://127.0.0.1:" + COURIER_FALSO.getAddress().getPort());
    }

    @BeforeEach
    void limpiarPeticiones() {
        PETICIONES.clear();
    }

    @Test
    @DisplayName("E2E HTTP: registra el envío, asigna un repartidor real y lo entrega")
    void flujoCompletoSobreHttpReal() throws Exception {
        String respuesta = mockMvc.perform(post(ENVIOS)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_ENVIO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn().getResponse().getContentAsString();
        String envioId = JsonPath.read(respuesta, "$.id");

        // El service consulta la disponibilidad al courier falso por HTTP (sin mocks).
        mockMvc.perform(put(ENVIOS + "/{id}/repartidor", envioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repartidorId\": \"%s\"}".formatted(REPARTIDOR_ACTIVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repartidorId").value(REPARTIDOR_ACTIVO.toString()));

        assertThat(PETICIONES)
                .containsExactly("GET /api/v1/repartidores/" + REPARTIDOR_ACTIVO + "/disponibilidad");

        mockMvc.perform(patch(ENVIOS + "/{id}/estado", envioId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\": \"EN_RUTA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_RUTA"));

        mockMvc.perform(patch(ENVIOS + "/{id}/estado", envioId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\": \"ENTREGADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"));

        mockMvc.perform(get(ENVIOS + "/{id}/historial", envioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].estadoNuevo").value("ENTREGADO"))
                .andExpect(jsonPath("$[2].estadoNuevo").value("PENDIENTE"));
    }

    @Test
    @DisplayName("E2E HTTP: un repartidor INACTIVO en courier-service no se asigna (422)")
    void repartidorInactivoNoSeAsigna() throws Exception {
        String envioId = registrarEnvio();

        mockMvc.perform(put(ENVIOS + "/{id}/repartidor", envioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repartidorId\": \"%s\"}".formatted(REPARTIDOR_INACTIVO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));

        mockMvc.perform(get(ENVIOS + "/{id}", envioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repartidorId").doesNotExist());

        assertThat(PETICIONES).hasSize(1);
        assertThat(PETICIONES.getFirst()).contains(REPARTIDOR_INACTIVO.toString());
    }

    @Test
    @DisplayName("E2E HTTP: si courier-service responde 404, el envío no se modifica")
    void repartidorDesconocidoDevuelve404() throws Exception {
        String envioId = registrarEnvio();

        mockMvc.perform(put(ENVIOS + "/{id}/repartidor", envioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repartidorId\": \"%s\"}".formatted(REPARTIDOR_DESCONOCIDO)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(ENVIOS + "/{id}", envioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.repartidorId").doesNotExist());
    }

    private String registrarEnvio() throws Exception {
        String respuesta = mockMvc.perform(post(ENVIOS)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_ENVIO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    // ------------------------------------------------------------------
    // courier-service falso: solo implementa el endpoint de disponibilidad
    // ------------------------------------------------------------------

    private static HttpServer iniciarCourierFalso() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/repartidores", FlujoCompletoHttpE2ETest::atender);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
            return server;
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo levantar el courier-service falso", ex);
        }
    }

    private static void atender(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        PETICIONES.add(exchange.getRequestMethod() + " " + path);

        Matcher matcher = RUTA_DISPONIBILIDAD.matcher(path);
        if (!"GET".equals(exchange.getRequestMethod()) || !matcher.matches()) {
            responder(exchange, 404, "{}");
            return;
        }

        UUID repartidorId = UUID.fromString(matcher.group(1));
        if (repartidorId.equals(REPARTIDOR_DESCONOCIDO)) {
            responder(exchange, 404, "{\"mensaje\": \"repartidor inexistente\"}");
            return;
        }

        boolean activo = !repartidorId.equals(REPARTIDOR_INACTIVO);
        responder(exchange, 200, """
                {"repartidorId": "%s", "estado": "%s", "disponible": %s}
                """.formatted(repartidorId, activo ? "ACTIVO" : "INACTIVO", activo));
    }

    private static void responder(HttpExchange exchange, int codigo, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(codigo, bytes.length);
        try (var salida = exchange.getResponseBody()) {
            salida.write(bytes);
        }
    }
}
