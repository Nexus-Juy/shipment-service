package ar.edu.unju.fi.logitrack.shipment.controller;

import ar.edu.unju.fi.logitrack.shipment.dto.asignacion.AsignacionResponse;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;
import ar.edu.unju.fi.logitrack.shipment.exception.BusinessRuleException;
import ar.edu.unju.fi.logitrack.shipment.exception.CourierServiceUnavailableException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import ar.edu.unju.fi.logitrack.shipment.service.AsignacionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AsignacionController.class)
class AsignacionControllerTest {

    private static final String URL = "/api/v1/envios/{envioId}/repartidor";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AsignacionService asignacionService;

    private final UUID envioId = UUID.randomUUID();
    private final UUID repartidorId = UUID.randomUUID();

    private String body() {
        return "{\"repartidorId\":\"" + repartidorId + "\"}";
    }

    @Test
    @DisplayName("PUT: 200 con el envío y el repartidor asignado")
    void asignarDevuelve200() throws Exception {
        given(asignacionService.asignarRepartidor(envioId, repartidorId))
                .willReturn(new AsignacionResponse(envioId, "LT-2026-000001", EstadoEnvio.PENDIENTE, repartidorId));

        mockMvc.perform(put(URL, envioId).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.envioId").value(envioId.toString()))
                .andExpect(jsonPath("$.estadoEnvio").value("PENDIENTE"))
                .andExpect(jsonPath("$.repartidorId").value(repartidorId.toString()));
    }

    @Test
    @DisplayName("PUT: 400 si falta el repartidorId")
    void asignarSinRepartidorDevuelve400() throws Exception {
        mockMvc.perform(put(URL, envioId).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.repartidorId").exists());
    }

    @Test
    @DisplayName("PUT: 400 si el envioId de la ruta no es un UUID")
    void asignarConEnvioIdInvalido() throws Exception {
        mockMvc.perform(put(URL, "xyz").contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT: 404 si el envío o el repartidor no existen")
    void asignarRecursoInexistenteDevuelve404() throws Exception {
        given(asignacionService.asignarRepartidor(envioId, repartidorId))
                .willThrow(ResourceNotFoundException.of("Envio", envioId));

        mockMvc.perform(put(URL, envioId).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT: 422 si el repartidor está INACTIVO")
    void asignarRepartidorInactivoDevuelve422() throws Exception {
        given(asignacionService.asignarRepartidor(envioId, repartidorId))
                .willThrow(new BusinessRuleException("El repartidor %s no está ACTIVO (estado: INACTIVO).".formatted(repartidorId)));

        mockMvc.perform(put(URL, envioId).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("PUT: 422 si el envío no está PENDIENTE")
    void asignarEnvioNoPendienteDevuelve422() throws Exception {
        given(asignacionService.asignarRepartidor(envioId, repartidorId))
                .willThrow(new BusinessRuleException("Solo se puede asignar repartidor a envíos PENDIENTES."));

        mockMvc.perform(put(URL, envioId).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("PUT: 503 si courier-service no está disponible")
    void asignarConCourierCaidoDevuelve503() throws Exception {
        given(asignacionService.asignarRepartidor(envioId, repartidorId))
                .willThrow(new CourierServiceUnavailableException("sin conexión", null));

        mockMvc.perform(put(URL, envioId).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }
}
