package ar.edu.unju.fi.logitrack.shipment.controller;

import ar.edu.unju.fi.logitrack.shipment.dto.repartidor.CambioEstadoRepartidorRequest;
import ar.edu.unju.fi.logitrack.shipment.dto.repartidor.RepartidorRequest;
import ar.edu.unju.fi.logitrack.shipment.dto.repartidor.RepartidorResponse;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoRepartidor;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.TipoVehiculo;
import ar.edu.unju.fi.logitrack.shipment.exception.BusinessRuleException;
import ar.edu.unju.fi.logitrack.shipment.exception.DuplicateResourceException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import ar.edu.unju.fi.logitrack.shipment.service.RepartidorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test de la capa web: el servicio está mockeado, se valida contrato HTTP y manejo de errores. */
@WebMvcTest(RepartidorController.class)
class RepartidorControllerTest {

    private static final String URL = "/api/v1/repartidores";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RepartidorService repartidorService;

    @Test
    void registrar_devuelve201ConLocation() throws Exception {
        UUID id = UUID.randomUUID();
        given(repartidorService.registrar(any())).willReturn(respuesta(id, EstadoRepartidor.ACTIVO));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RepartidorRequest("Ana Torres", TipoVehiculo.MOTO, "ana.torres@logitrack.test", "+54 388 400-5000"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(URL + "/" + id)))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void registrar_conDatosInvalidos_devuelve400ConDetalleDeCampos() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\", \"tipoVehiculo\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errors.nombre").exists())
                .andExpect(jsonPath("$.errors.tipoVehiculo").exists());
    }

    @Test
    void registrar_conTipoVehiculoInexistente_devuelve400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Ana\", \"tipoVehiculo\": \"AVION\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listar_filtraPorEstado() throws Exception {
        given(repartidorService.listar(EstadoRepartidor.ACTIVO))
                .willReturn(List.of(respuesta(UUID.randomUUID(), EstadoRepartidor.ACTIVO)));

        mockMvc.perform(get(URL).param("estado", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estado").value("ACTIVO"));
    }

    @Test
    void obtener_inexistente_devuelve404() throws Exception {
        UUID id = UUID.randomUUID();
        given(repartidorService.obtenerPorId(id)).willThrow(ResourceNotFoundException.of("Repartidor", id));

        mockMvc.perform(get(URL + "/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void obtener_conIdQueNoEsUuid_devuelve400() throws Exception {
        mockMvc.perform(get(URL + "/no-es-un-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/repartidores/{id}/estado: 200 con el repartidor actualizado")
    void cambiarEstado_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        given(repartidorService.cambiarEstado(id, EstadoRepartidor.INACTIVO))
                .willReturn(respuesta(id, EstadoRepartidor.INACTIVO));

        mockMvc.perform(patch(URL + "/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambioEstadoRepartidorRequest(EstadoRepartidor.INACTIVO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    @DisplayName("PATCH: 400 si falta el estado en el body")
    void cambiarEstado_sinEstado_devuelve400() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch(URL + "/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH: 404 si el repartidor no existe")
    void cambiarEstado_inexistente_devuelve404() throws Exception {
        UUID id = UUID.randomUUID();
        given(repartidorService.cambiarEstado(id, EstadoRepartidor.INACTIVO))
                .willThrow(ResourceNotFoundException.of("Repartidor", id));

        mockMvc.perform(patch(URL + "/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambioEstadoRepartidorRequest(EstadoRepartidor.INACTIVO))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH: 422 si el repartidor tiene envíos EN_RUTA")
    void cambiarEstado_conEnviosEnRuta_devuelve422() throws Exception {
        UUID id = UUID.randomUUID();
        given(repartidorService.cambiarEstado(id, EstadoRepartidor.INACTIVO))
                .willThrow(new BusinessRuleException("No se puede desactivar al repartidor: tiene envíos EN_RUTA"));

        mockMvc.perform(patch(URL + "/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambioEstadoRepartidorRequest(EstadoRepartidor.INACTIVO))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("POST: 400 si falta el email o tiene un formato inválido")
    void registrar_emailInvalido_devuelve400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Ana\", \"tipoVehiculo\": \"MOTO\", \"email\": \"no-es-un-email\", \"telefono\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.telefono").exists());
    }

    @Test
    @DisplayName("POST: 409 si el email ya pertenece a otro repartidor")
    void registrar_emailDuplicado_devuelve409() throws Exception {
        given(repartidorService.registrar(any()))
                .willThrow(DuplicateResourceException.of("repartidor", "email", "ana.torres@logitrack.test"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RepartidorRequest("Ana Torres", TipoVehiculo.MOTO, "ana.torres@logitrack.test", "+54 388 400-5000"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    @DisplayName("PUT /api/v1/repartidores/{id}: 200 con los datos actualizados")
    void actualizar_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        given(repartidorService.actualizar(eq(id), any())).willReturn(respuesta(id, EstadoRepartidor.ACTIVO));

        mockMvc.perform(put(URL + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RepartidorRequest("Ana Torres", TipoVehiculo.MOTO, "ana.torres@logitrack.test", "+54 388 400-5000"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana.torres@logitrack.test"))
                .andExpect(jsonPath("$.telefono").value("+54 388 400-5000"));
    }

    @Test
    @DisplayName("PUT: 404 si el repartidor no existe")
    void actualizar_inexistente_devuelve404() throws Exception {
        UUID id = UUID.randomUUID();
        given(repartidorService.actualizar(eq(id), any())).willThrow(ResourceNotFoundException.of("Repartidor", id));

        mockMvc.perform(put(URL + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RepartidorRequest("Ana Torres", TipoVehiculo.MOTO, "ana.torres@logitrack.test", "+54 388 400-5000"))))
                .andExpect(status().isNotFound());
    }

    private static RepartidorResponse respuesta(UUID id, EstadoRepartidor estado) {
        Instant ahora = Instant.now();
        return new RepartidorResponse(id, "Ana Torres", TipoVehiculo.MOTO,
                "ana.torres@logitrack.test", "+54 388 400-5000", estado, ahora, ahora);
    }
}
