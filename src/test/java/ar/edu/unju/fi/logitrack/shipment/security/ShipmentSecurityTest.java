package ar.edu.unju.fi.logitrack.shipment.security;

import ar.edu.unju.fi.logitrack.shipment.client.CourierClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S2-06: verifica el resource server de {@code shipment-service} de punta a punta, firmando JWT
 * reales con el secreto de test (no usa {@code @WithMockUser}, porque lo que se prueba es la
 * validación del token y la traducción del claim {@code roles}).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ShipmentSecurityTest {

    private static final String ENVIOS = "/api/v1/envios";

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

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourierClient courierClient;

    @Test
    @DisplayName("Sin token, un endpoint de negocio responde 401 en formato ProblemDetail")
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get(ENVIOS))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("No autorizado"))
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Un token vencido se rechaza con 401")
    void tokenVencidoResponde401() throws Exception {
        mockMvc.perform(get(ENVIOS)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestSupport.tokenExpirado("ADMIN")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Un token firmado con otro secreto se rechaza con 401")
    void firmaInvalidaResponde401() throws Exception {
        mockMvc.perform(get(ENVIOS)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestSupport.tokenConOtroSecreto("ADMIN")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Con un token válido de CLIENTE se puede consultar el listado y registrar un envío")
    void clientePuedeConsultarYRegistrar() throws Exception {
        String token = JwtTestSupport.token("CLIENTE");

        mockMvc.perform(get(ENVIOS).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());

        mockMvc.perform(post(ENVIOS)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_ENVIO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("Un REPARTIDOR no puede crear un envío (403)")
    void repartidorNoPuedeCrearEnvio() throws Exception {
        mockMvc.perform(post(ENVIOS)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestSupport.token("REPARTIDOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_ENVIO))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Acceso denegado"))
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Un REPARTIDOR no puede cancelar ni reasignar un envío (403)")
    void repartidorNoPuedeCancelarNiAsignar() throws Exception {
        String token = JwtTestSupport.token("REPARTIDOR");
        UUID envioId = UUID.randomUUID();

        mockMvc.perform(post(ENVIOS + "/{id}/cancelacion", envioId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden());

        mockMvc.perform(put(ENVIOS + "/{id}/repartidor", envioId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repartidorId\": \"%s\"}".formatted(UUID.randomUUID())))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un REPARTIDOR sí puede cambiar el estado de un envío")
    void repartidorPuedeCambiarEstado() throws Exception {
        // El 404 confirma que pasó la seguridad y llegó al service: el envío no existe.
        mockMvc.perform(patch(ENVIOS + "/{id}/estado", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestSupport.token("REPARTIDOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": \"EN_RUTA\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un CLIENTE no puede cambiar el estado de un envío (403)")
    void clienteNoPuedeCambiarEstado() throws Exception {
        mockMvc.perform(patch(ENVIOS + "/{id}/estado", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestSupport.token("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": \"EN_RUTA\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un token sin roles no puede escribir (403)")
    void tokenSinRolesNoPuedeEscribir() throws Exception {
        mockMvc.perform(post(ENVIOS)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestSupport.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_ENVIO))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un token sin roles puede consultar (solo se exige autenticación)")
    void tokenSinRolesPuedeConsultar() throws Exception {
        mockMvc.perform(get(ENVIOS)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestSupport.token()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Salud y documentación quedan públicos para Docker y Swagger")
    void endpointsPublicosNoPidenToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openapi").exists());
    }
}
