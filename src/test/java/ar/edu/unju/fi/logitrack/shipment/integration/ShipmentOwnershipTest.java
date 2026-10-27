package ar.edu.unju.fi.logitrack.shipment.integration;

import ar.edu.unju.fi.logitrack.shipment.TestDataFactory;
import ar.edu.unju.fi.logitrack.shipment.client.CourierClient;
import ar.edu.unju.fi.logitrack.shipment.dto.courier.DisponibilidadResponse;
import ar.edu.unju.fi.logitrack.shipment.repository.ClienteRepository;
import ar.edu.unju.fi.logitrack.shipment.security.JwtTestSupport;
import com.jayway.jsonpath.JsonPath;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S2-07: autorización por <b>pertenencia</b>. Complementa a {@code ShipmentSecurityTest} (que cubre
 * el 401 y el 403 por rol) verificando que un {@code CLIENTE} solo llegue a sus envíos y un
 * {@code REPARTIDOR} solo a los que tiene asignados, con JWT reales y H2 en memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ShipmentOwnershipTest {

    private static final String ENVIOS = "/api/v1/envios";

    private static final UUID USUARIO_CLIENTE_A = UUID.randomUUID();
    private static final UUID USUARIO_CLIENTE_B = UUID.randomUUID();
    private static final UUID USUARIO_ADMIN = UUID.randomUUID();

    private static final UUID REPARTIDOR_UNO = TestDataFactory.REPARTIDOR_ID;
    private static final UUID REPARTIDOR_DOS = UUID.fromString("3f6c1a52-8e0b-4d7a-9c11-0a1b2c3d4e02");

    private static final String TOKEN_CLIENTE_A = JwtTestSupport.tokenDe(USUARIO_CLIENTE_A, "CLIENTE");
    private static final String TOKEN_CLIENTE_B = JwtTestSupport.tokenDe(USUARIO_CLIENTE_B, "CLIENTE");
    private static final String TOKEN_ADMIN = JwtTestSupport.tokenDe(USUARIO_ADMIN, "ADMIN");
    private static final String TOKEN_REPARTIDOR_UNO = JwtTestSupport.tokenDeRepartidor(REPARTIDOR_UNO, "REPARTIDOR");
    private static final String TOKEN_REPARTIDOR_DOS = JwtTestSupport.tokenDeRepartidor(REPARTIDOR_DOS, "REPARTIDOR");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @MockitoBean
    private CourierClient courierClient;

    // ---------------------------------------------------------------- CLIENTE

    @Test
    @DisplayName("Un CLIENTE puede consultar su propio envío")
    void clientePuedeConsultarSuEnvio() throws Exception {
        String envioId = registrarEnvio(TOKEN_CLIENTE_A);

        mockMvc.perform(get(ENVIOS + "/{id}", envioId).header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_CLIENTE_A)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(envioId));
    }

    @Test
    @DisplayName("Un CLIENTE recibe 403 al consultar el envío de otro cliente")
    void clienteNoPuedeConsultarEnvioAjeno() throws Exception {
        String envioAjeno = registrarEnvio(TOKEN_CLIENTE_A);

        mockMvc.perform(get(ENVIOS + "/{id}", envioAjeno).header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_CLIENTE_B)))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Acceso denegado"))
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Un CLIENTE recibe 403 al seguir por código un envío ajeno")
    void clienteNoPuedeSeguirEnvioAjenoPorCodigo() throws Exception {
        String codigo = registrarEnvioYLeerCodigo(TOKEN_CLIENTE_A);

        mockMvc.perform(get(ENVIOS + "/seguimiento/{codigo}", codigo)
                .header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_CLIENTE_B)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un CLIENTE solo ve sus envíos en el listado")
    void clienteSoloListaSusEnvios() throws Exception {
        String propio = registrarEnvio(TOKEN_CLIENTE_A);
        String ajeno = registrarEnvio(TOKEN_CLIENTE_B);

        // El contexto se comparte entre tests, así que el CLIENTE A puede tener más de un envío:
        // lo que importa es que el de B nunca aparezca.
        mockMvc.perform(get(ENVIOS).header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_CLIENTE_A)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == '" + propio + "')]").exists())
            .andExpect(jsonPath("$[?(@.id == '" + ajeno + "')]").doesNotExist());
    }

    @Test
    @DisplayName("Un CLIENTE puede cancelar su envío y recibe 403 con el de otro")
    void clienteCancelaSoloLoPropio() throws Exception {
        String propio = registrarEnvio(TOKEN_CLIENTE_A);
        String ajeno = registrarEnvio(TOKEN_CLIENTE_B);

        mockMvc.perform(post(ENVIOS + "/{id}/cancelacion", ajeno)
                .header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_CLIENTE_A)))
            .andExpect(status().isForbidden());

        mockMvc.perform(post(ENVIOS + "/{id}/cancelacion", propio)
                .header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_CLIENTE_A)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADO"));
    }

    @Test
    @DisplayName("Un CLIENTE recibe 403 al pedir el historial de un envío ajeno")
    void clienteNoPuedeVerHistorialAjeno() throws Exception {
        String propio = registrarEnvio(TOKEN_CLIENTE_A);
        String ajeno = registrarEnvio(TOKEN_CLIENTE_B);

        mockMvc.perform(get(ENVIOS + "/{id}/historial", ajeno)
                .header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_CLIENTE_A)))
            .andExpect(status().isForbidden());

        mockMvc.perform(get(ENVIOS + "/{id}/historial", propio)
                .header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_CLIENTE_A)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Un CLIENTE no puede asignar repartidor a un envío ajeno (y no se llama a courier)")
    void clienteNoPuedeAsignarEnvioAjeno() throws Exception {
        String ajeno = registrarEnvio(TOKEN_CLIENTE_B);
        when(courierClient.consultarDisponibilidad(any()))
            .thenReturn(new DisponibilidadResponse(REPARTIDOR_UNO, "ACTIVO", true));

        mockMvc.perform(put(ENVIOS + "/{id}/repartidor", ajeno)
                .header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_CLIENTE_A))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repartidorId\": \"%s\"}".formatted(REPARTIDOR_UNO)))
            .andExpect(status().isForbidden());

        verify(courierClient, never()).consultarDisponibilidad(any());
    }

    @Test
    @DisplayName("Un CLIENTE sí puede asignar repartidor a su propio envío")
    void clientePuedeAsignarSuEnvio() throws Exception {
        String propio = registrarEnvio(TOKEN_CLIENTE_A);
        when(courierClient.consultarDisponibilidad(REPARTIDOR_UNO))
            .thenReturn(new DisponibilidadResponse(REPARTIDOR_UNO, "ACTIVO", true));

        mockMvc.perform(put(ENVIOS + "/{id}/repartidor", propio)
                .header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_CLIENTE_A))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repartidorId\": \"%s\"}".formatted(REPARTIDOR_UNO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.repartidorId").value(REPARTIDOR_UNO.toString()));
    }

    @Test
    @DisplayName("Al registrar un envío, el cliente queda vinculado al usuario del token")
    void registrarEnvioVinculaElClienteConElUsuario() throws Exception {
        String email = emailUnico();
        registrarEnvio(TOKEN_CLIENTE_A, email);

        assertThat(clienteRepository.findByEmailIgnoreCase(email).orElseThrow().getUsuarioId())
            .isEqualTo(USUARIO_CLIENTE_A);
    }

    @Test
    @DisplayName("Un CLIENTE no puede registrar un envío a nombre del cliente de otro")
    void clienteNoPuedeUsarUnClienteAjeno() throws Exception {
        String emailA = emailUnico();
        registrarEnvio(TOKEN_CLIENTE_A, emailA);
        UUID clienteIdA = clienteRepository.findByEmailIgnoreCase(emailA).orElseThrow().getId();

        mockMvc.perform(post(ENVIOS)
                .header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_CLIENTE_B))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "clienteId": "%s",
                      "origenCalle": "Belgrano", "origenNumero": "450",
                      "origenCiudad": "San Salvador de Jujuy", "origenCodigoPostal": "4600",
                      "destinoCalle": "Av. Fascio", "destinoNumero": "1200",
                      "destinoCiudad": "San Salvador de Jujuy", "destinoCodigoPostal": "4600",
                      "peso": 2.5, "ancho": 30, "alto": 20, "largo": 10
                    }
                    """.formatted(clienteIdA)))
            .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ REPARTIDOR

    @Test
    @DisplayName("Un REPARTIDOR consulta solo los envíos que tiene asignados")
    void repartidorConsultaSoloSusEnvios() throws Exception {
        String asignado = registrarEnvioYAsignar(REPARTIDOR_UNO);
        String deOtro = registrarEnvioYAsignar(REPARTIDOR_DOS);

        mockMvc.perform(get(ENVIOS + "/{id}", asignado)
                .header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_REPARTIDOR_UNO)))
            .andExpect(status().isOk());

        mockMvc.perform(get(ENVIOS + "/{id}", deOtro)
                .header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_REPARTIDOR_UNO)))
            .andExpect(status().isForbidden());

        mockMvc.perform(get(ENVIOS).header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_REPARTIDOR_UNO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == '" + asignado + "')]").exists())
            .andExpect(jsonPath("$[?(@.id == '" + deOtro + "')]").doesNotExist());
    }

    @Test
    @DisplayName("Un REPARTIDOR recibe 403 al cambiar el estado de un envío no asignado")
    void repartidorNoPuedeCambiarEstadoDeEnvioNoAsignado() throws Exception {
        String deOtro = registrarEnvioYAsignar(REPARTIDOR_DOS);

        mockMvc.perform(patch(ENVIOS + "/{id}/estado", deOtro)
                .header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_REPARTIDOR_UNO))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": \"EN_RUTA\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un REPARTIDOR sí puede cambiar el estado de un envío asignado")
    void repartidorPuedeCambiarEstadoDeSuEnvio() throws Exception {
        String asignado = registrarEnvioYAsignar(REPARTIDOR_UNO);

        mockMvc.perform(patch(ENVIOS + "/{id}/estado", asignado)
                .header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_REPARTIDOR_UNO))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": \"EN_RUTA\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("EN_RUTA"));
    }

    @Test
    @DisplayName("Un REPARTIDOR no puede pedir la hoja de ruta de otro repartidor")
    void repartidorNoPuedeConsultarHojaDeRutaAjena() throws Exception {
        mockMvc.perform(get(ENVIOS).param("repartidorId", REPARTIDOR_DOS.toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_REPARTIDOR_UNO)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un token de REPARTIDOR sin claim repartidorId no puede listar envíos")
    void repartidorSinClaimNoPuedeListar() throws Exception {
        String tokenSinClaim = JwtTestSupport.token("REPARTIDOR");

        mockMvc.perform(get(ENVIOS).header(HttpHeaders.AUTHORIZATION, bearer(tokenSinClaim)))
            .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- ADMIN

    @Test
    @DisplayName("Un ADMIN mantiene acceso total a cualquier envío")
    void adminAccedeATodo() throws Exception {
        String deCliente = registrarEnvio(TOKEN_CLIENTE_B);

        mockMvc.perform(get(ENVIOS + "/{id}", deCliente).header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_ADMIN)))
            .andExpect(status().isOk());

        mockMvc.perform(get(ENVIOS + "/{id}/historial", deCliente)
                .header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_ADMIN)))
            .andExpect(status().isOk());

        mockMvc.perform(post(ENVIOS + "/{id}/cancelacion", deCliente)
                .header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADO"));
    }

    // -------------------------------------------------------------- helpers

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String emailUnico() {
        return "cliente." + UUID.randomUUID() + "@logitrack.test";
    }

    private static String bodyEnvio(String email) {
        return """
            {
              "origenCalle": "Belgrano", "origenNumero": "450",
              "origenCiudad": "San Salvador de Jujuy", "origenCodigoPostal": "4600",
              "destinoCalle": "Av. Fascio", "destinoNumero": "1200",
              "destinoCiudad": "San Salvador de Jujuy", "destinoCodigoPostal": "4600",
              "peso": 2.5, "ancho": 30, "alto": 20, "largo": 10,
              "cliente": {"nombre": "Cliente de prueba", "email": "%s"}
            }
            """.formatted(email);
    }

    private String registrarEnvio(String token) throws Exception {
        return registrarEnvio(token, emailUnico());
    }

    private String registrarEnvio(String token, String email) throws Exception {
        String respuesta = mockMvc.perform(post(ENVIOS)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyEnvio(email)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    private String registrarEnvioYLeerCodigo(String token) throws Exception {
        String respuesta = mockMvc.perform(post(ENVIOS)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyEnvio(emailUnico())))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.codigoSeguimiento");
    }

    /** Crea un envío como ADMIN y lo asigna al repartidor indicado (con courier mockeado). */
    private String registrarEnvioYAsignar(UUID repartidorId) throws Exception {
        String envioId = registrarEnvio(TOKEN_ADMIN);
        when(courierClient.consultarDisponibilidad(repartidorId))
            .thenReturn(new DisponibilidadResponse(repartidorId, "ACTIVO", true));

        mockMvc.perform(put(ENVIOS + "/{id}/repartidor", envioId)
                .header(HttpHeaders.AUTHORIZATION, bearer(TOKEN_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repartidorId\": \"%s\"}".formatted(repartidorId)))
            .andExpect(status().isOk());

        return envioId;
    }
}
