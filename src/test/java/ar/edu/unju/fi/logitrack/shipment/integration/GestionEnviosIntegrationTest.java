package ar.edu.unju.fi.logitrack.shipment.integration;

import ar.edu.unju.fi.logitrack.shipment.TestDataFactory;
import ar.edu.unju.fi.logitrack.shipment.client.CourierClient;
import ar.edu.unju.fi.logitrack.shipment.dto.courier.DisponibilidadResponse;
import ar.edu.unju.fi.logitrack.shipment.repository.ClienteRepository;
import ar.edu.unju.fi.logitrack.shipment.repository.EnvioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración de punta a punta del ciclo de vida de un envío:
 * controller + service + repository + H2 en memoria.
 * <p>
 * courier-service se reemplaza por un mock: shipment ya no tiene la entidad Repartidor y su
 * disponibilidad se consulta por REST.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class GestionEnviosIntegrationTest {

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

    @Autowired
    private EnvioRepository envioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @MockitoBean
    private CourierClient courierClient;

    private final UUID repartidorId = TestDataFactory.REPARTIDOR_ID;

    @BeforeEach
    void limpiarBase() {
        envioRepository.deleteAll();
        clienteRepository.deleteAll();
    }

    private void repartidorDisponible(boolean disponible) {
        given(courierClient.consultarDisponibilidad(repartidorId))
                .willReturn(new DisponibilidadResponse(repartidorId, disponible ? "ACTIVO" : "INACTIVO", disponible));
    }

    @Test
    @DisplayName("Flujo completo: registrar, asignar, despachar, entregar y auditar cada cambio")
    void cicloDeVidaCompleto() throws Exception {
        String respuesta = mockMvc.perform(post(ENVIOS).contentType(MediaType.APPLICATION_JSON).content(BODY_ENVIO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.tarifa").value(2625.00))
            .andReturn().getResponse().getContentAsString();
        String envioId = JsonPath.read(respuesta, "$.id");
        String codigo = JsonPath.read(respuesta, "$.codigoSeguimiento");

        mockMvc.perform(get(ENVIOS + "/seguimiento/{codigo}", codigo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(envioId));

        // Sin repartidor asignado no puede pasar a EN_RUTA
        mockMvc.perform(patch(ENVIOS + "/{id}/estado", envioId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\": \"EN_RUTA\"}"))
            .andExpect(status().isUnprocessableEntity());

        // courier-service confirma que el repartidor está ACTIVO
        repartidorDisponible(true);

        mockMvc.perform(put(ENVIOS + "/{id}/repartidor", envioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repartidorId\": \"%s\"}".formatted(repartidorId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.repartidorId").value(repartidorId.toString()));

        mockMvc.perform(patch(ENVIOS + "/{id}/estado", envioId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\": \"EN_RUTA\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("EN_RUTA"));

        // Ya en ruta: no se puede cancelar
        mockMvc.perform(post(ENVIOS + "/{id}/cancelacion", envioId))
            .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch(ENVIOS + "/{id}/estado", envioId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\": \"ENTREGADO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("ENTREGADO"));

        // La hoja de ruta del repartidor muestra el envío entregado
        mockMvc.perform(get(ENVIOS).param("repartidorId", repartidorId.toString()).param("estado", "ENTREGADO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(envioId))
            .andExpect(jsonPath("$[0].repartidorId").value(repartidorId.toString()));

        mockMvc.perform(get(ENVIOS + "/{id}/historial", envioId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].estadoNuevo").value("ENTREGADO"))
            .andExpect(jsonPath("$[2].estadoNuevo").value("PENDIENTE"));
    }

    @Test
    @DisplayName("No asigna un repartidor que courier-service informa como INACTIVO")
    void noAsignaRepartidorInactivo() throws Exception {
        String respuesta = mockMvc.perform(post(ENVIOS).contentType(MediaType.APPLICATION_JSON).content(BODY_ENVIO))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String envioId = JsonPath.read(respuesta, "$.id");

        repartidorDisponible(false);

        mockMvc.perform(put(ENVIOS + "/{id}/repartidor", envioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repartidorId\": \"%s\"}".formatted(repartidorId)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.status").value(422));

        mockMvc.perform(get(ENVIOS + "/{id}", envioId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.repartidorId").doesNotExist());
    }

    @Test
    @DisplayName("Un envío PENDIENTE se cancela y queda registrado en el historial")
    void cancelarEnvioPendiente() throws Exception {
        String respuesta = mockMvc.perform(post(ENVIOS).contentType(MediaType.APPLICATION_JSON).content(BODY_ENVIO))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String envioId = JsonPath.read(respuesta, "$.id");

        mockMvc.perform(post(ENVIOS + "/{id}/cancelacion", envioId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADO"));

        mockMvc.perform(get(ENVIOS).param("estado", "CANCELADO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get(ENVIOS + "/{id}/historial", envioId))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].estadoNuevo").value("CANCELADO"));
    }

    @Test
    @DisplayName("Dos envíos del mismo cliente por email reutilizan el cliente y tienen códigos distintos")
    void clienteSeReutilizaPorEmail() throws Exception {
        mockMvc.perform(post(ENVIOS).contentType(MediaType.APPLICATION_JSON).content(BODY_ENVIO))
            .andExpect(status().isCreated());
        mockMvc.perform(post(ENVIOS).contentType(MediaType.APPLICATION_JSON).content(BODY_ENVIO))
            .andExpect(status().isCreated());

        org.assertj.core.api.Assertions.assertThat(clienteRepository.count()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(envioRepository.count()).isEqualTo(2);
    }
}
