package ar.edu.unju.fi.logitrack.shipment.controller;

import ar.edu.unju.fi.logitrack.shipment.dto.envio.DireccionResponse;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.EnvioResponse;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.HistorialEstadoResponse;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;
import ar.edu.unju.fi.logitrack.shipment.exception.BusinessRuleException;
import ar.edu.unju.fi.logitrack.shipment.exception.InvalidStateTransitionException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import ar.edu.unju.fi.logitrack.shipment.service.ShipmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test de la capa web de envíos: el servicio está mockeado, se valida el contrato HTTP y el manejo de errores. */
@WebMvcTest(ShipmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShipmentControllerTest {

    private static final String URL = "/api/v1/envios";

    private static final String BODY_VALIDO = """
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
    private ShipmentService shipmentService;

    @Test
    @DisplayName("POST /envios válido devuelve 201 con Location y el envío PENDIENTE")
    void registrar_devuelve201ConLocation() throws Exception {
        UUID id = UUID.randomUUID();
        given(shipmentService.registrarEnvio(any())).willReturn(respuesta(id, EstadoEnvio.PENDIENTE));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", endsWith(URL + "/" + id)))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.tarifa").value(2625.00))
            .andExpect(jsonPath("$.origen.ciudad").value("San Salvador de Jujuy"));
    }

    @Test
    @DisplayName("POST /envios con campos inválidos devuelve 400 con el detalle por campo")
    void registrar_conDatosInvalidos_devuelve400() throws Exception {
        String bodyInvalido = """
            {"origenCalle": "", "peso": -1, "ancho": 30, "alto": 20, "largo": 10}
            """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(bodyInvalido))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Datos inválidos"))
            .andExpect(jsonPath("$.errors.origenCalle").exists())
            .andExpect(jsonPath("$.errors.destinoCalle").exists())
            .andExpect(jsonPath("$.errors.peso").exists());
    }

    @Test
    @DisplayName("POST /envios sin cliente devuelve 422 por regla de negocio")
    void registrar_sinCliente_devuelve422() throws Exception {
        given(shipmentService.registrarEnvio(any()))
            .willThrow(new BusinessRuleException("Para registrar un envío hay que indicar el clienteId o los datos del cliente."));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.title").value("Regla de negocio violada"));
    }

    @Test
    @DisplayName("GET /envios/{id} inexistente devuelve 404")
    void obtenerPorId_inexistente_devuelve404() throws Exception {
        UUID id = UUID.randomUUID();
        given(shipmentService.obtenerPorId(id)).willThrow(ResourceNotFoundException.of("Envío", id));

        mockMvc.perform(get(URL + "/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    @DisplayName("GET /envios/{id} con un id que no es UUID devuelve 400")
    void obtenerPorId_conIdInvalido_devuelve400() throws Exception {
        mockMvc.perform(get(URL + "/no-es-un-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /envios/seguimiento/{codigo} devuelve el envío")
    void obtenerPorCodigo_devuelve200() throws Exception {
        given(shipmentService.obtenerPorCodigoSeguimiento("LT-ABC123"))
            .willReturn(respuesta(UUID.randomUUID(), EstadoEnvio.PENDIENTE));

        mockMvc.perform(get(URL + "/seguimiento/{codigo}", "LT-ABC123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigoSeguimiento").value("LT-TEST-001"));
    }

    @Test
    @DisplayName("GET /envios?estado=EN_RUTA filtra por estado")
    void listar_filtraPorEstado() throws Exception {
        given(shipmentService.listar(EstadoEnvio.EN_RUTA, null))
            .willReturn(List.of(respuesta(UUID.randomUUID(), EstadoEnvio.EN_RUTA)));

        mockMvc.perform(get(URL).param("estado", "EN_RUTA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].estado").value("EN_RUTA"));
    }

    @Test
    @DisplayName("GET /envios?repartidorId=... devuelve la hoja de ruta de ese repartidor")
    void listar_filtraPorRepartidor() throws Exception {
        UUID repartidorId = UUID.randomUUID();
        given(shipmentService.listar(null, repartidorId))
            .willReturn(List.of(respuesta(UUID.randomUUID(), EstadoEnvio.EN_RUTA)));

        mockMvc.perform(get(URL).param("repartidorId", repartidorId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /envios con un repartidorId que no es UUID devuelve 400")
    void listar_conRepartidorIdInvalido_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("repartidorId", "abc"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /envios con un estado inexistente devuelve 400")
    void listar_conEstadoInexistente_devuelve400() throws Exception {
        mockMvc.perform(get(URL).param("estado", "VOLANDO"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /envios/{id}/estado con una transición válida devuelve 200")
    void cambiarEstado_valido_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        given(shipmentService.cambiarEstado(any(), any())).willReturn(respuesta(id, EstadoEnvio.EN_RUTA));

        mockMvc.perform(patch(URL + "/{id}/estado", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": \"EN_RUTA\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("EN_RUTA"));
    }

    @Test
    @DisplayName("PATCH /envios/{id}/estado con una transición inválida devuelve 422")
    void cambiarEstado_transicionInvalida_devuelve422() throws Exception {
        UUID id = UUID.randomUUID();
        given(shipmentService.cambiarEstado(any(), any()))
            .willThrow(new InvalidStateTransitionException(EstadoEnvio.PENDIENTE, EstadoEnvio.ENTREGADO));

        mockMvc.perform(patch(URL + "/{id}/estado", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": \"ENTREGADO\"}"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PATCH /envios/{id}/estado sin estado devuelve 400")
    void cambiarEstado_sinEstado_devuelve400() throws Exception {
        mockMvc.perform(patch(URL + "/{id}/estado", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.estado").exists());
    }

    @Test
    @DisplayName("POST /envios/{id}/cancelacion de un envío PENDIENTE devuelve 200 CANCELADO")
    void cancelar_pendiente_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        given(shipmentService.cancelarEnvio(id)).willReturn(respuesta(id, EstadoEnvio.CANCELADO));

        mockMvc.perform(post(URL + "/{id}/cancelacion", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADO"));
    }

    @Test
    @DisplayName("POST /envios/{id}/cancelacion de un envío que no está PENDIENTE devuelve 422")
    void cancelar_noPendiente_devuelve422() throws Exception {
        UUID id = UUID.randomUUID();
        given(shipmentService.cancelarEnvio(id))
            .willThrow(new BusinessRuleException("solo se cancela en PENDIENTE"));

        mockMvc.perform(post(URL + "/{id}/cancelacion", id))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("GET /envios/{id}/historial devuelve los cambios de estado")
    void historial_devuelveRegistros() throws Exception {
        UUID id = UUID.randomUUID();
        given(shipmentService.obtenerHistorial(id)).willReturn(List.of(
            new HistorialEstadoResponse(EstadoEnvio.PENDIENTE, EstadoEnvio.EN_RUTA, Instant.parse("2026-10-25T12:00:00Z")),
            new HistorialEstadoResponse(null, EstadoEnvio.PENDIENTE, Instant.parse("2026-10-25T10:00:00Z"))));

        mockMvc.perform(get(URL + "/{id}/historial", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].estadoNuevo").value("EN_RUTA"))
            .andExpect(jsonPath("$[1].estadoAnterior").doesNotExist());
    }

    private EnvioResponse respuesta(UUID id, EstadoEnvio estado) {
        return new EnvioResponse(
            id, "LT-TEST-001", estado, UUID.randomUUID(), null,
            new DireccionResponse("Belgrano", "450", "San Salvador de Jujuy", "4600"),
            new DireccionResponse("Av. Fascio", "1200", "San Salvador de Jujuy", "4600"),
            new BigDecimal("2.5"), new BigDecimal("20"), new BigDecimal("30"), new BigDecimal("10"),
            new BigDecimal("2625.00"), Instant.parse("2026-10-25T10:00:00Z"));
    }
}
