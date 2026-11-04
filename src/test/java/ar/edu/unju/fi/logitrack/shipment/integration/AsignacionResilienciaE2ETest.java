package ar.edu.unju.fi.logitrack.shipment.integration;

import ar.edu.unju.fi.logitrack.shipment.security.JwtTestSupport;
import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S3-04: cuando courier-service falla, la asignación responde un 503 estructurado (nunca un 500) y,
 * tras varias fallas seguidas, el circuit breaker se abre y deja de llamarlo.
 * <p>
 * Usa un courier-service falso sobre HTTP real que siempre responde {@code 500}. Tiene su propio
 * contexto de Spring (otra propiedad dinámica), así que el circuit breaker no se comparte con
 * {@link FlujoCompletoHttpE2ETest}.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class AsignacionResilienciaE2ETest {

    private static final String ENVIOS = "/api/v1/envios";
    private static final String TOKEN = JwtTestSupport.token("ADMIN");
    private static final UUID REPARTIDOR = UUID.fromString("3f6c1a52-8e0b-4d7a-9c11-0a1b2c3d4e01");

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

    /** Cantidad de consultas que llegaron al courier falso. */
    private static final AtomicInteger LLAMADAS = new AtomicInteger();

    private static final HttpServer COURIER_FALSO = iniciarCourierQueFalla();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void apuntarAlCourierFalso(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.courier-service[0].uri",
                () -> "http://127.0.0.1:" + COURIER_FALSO.getAddress().getPort());
    }

    @Test
    @DisplayName("Courier responde 500: la asignación da 503 estructurado y, con el circuito abierto, ya no lo llama")
    void courierFallaYElCircuitoSeAbre() throws Exception {
        String respuesta = mockMvc.perform(post(ENVIOS)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_ENVIO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String envioId = JsonPath.read(respuesta, "$.id");
        LLAMADAS.set(0);

        // Las primeras 5 llamadas llegan al courier falso, que responde 500: el servicio responde 503.
        for (int i = 0; i < 5; i++) {
            asignar(envioId)
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value(503));
        }
        assertThat(LLAMADAS.get()).isEqualTo(5);

        // Con el circuito abierto responde 503 de inmediato, sin llegar al courier.
        asignar(envioId)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
        assertThat(LLAMADAS.get()).isEqualTo(5);
    }

    private org.springframework.test.web.servlet.ResultActions asignar(String envioId) throws Exception {
        return mockMvc.perform(put(ENVIOS + "/{id}/repartidor", envioId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repartidorId\": \"%s\"}".formatted(REPARTIDOR)));
    }

    private static HttpServer iniciarCourierQueFalla() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/repartidores", AsignacionResilienciaE2ETest::responder500);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
            return server;
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo levantar el courier-service falso", ex);
        }
    }

    private static void responder500(HttpExchange exchange) throws IOException {
        LLAMADAS.incrementAndGet();
        byte[] bytes = "{\"error\": \"falla simulada\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.sendResponseHeaders(500, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
