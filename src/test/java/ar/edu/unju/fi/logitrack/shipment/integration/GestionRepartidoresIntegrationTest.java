package ar.edu.unju.fi.logitrack.shipment.integration;

import com.jayway.jsonpath.JsonPath;
import ar.edu.unju.fi.logitrack.shipment.TestDataFactory;
import ar.edu.unju.fi.logitrack.shipment.entity.Cliente;
import ar.edu.unju.fi.logitrack.shipment.entity.Envio;
import ar.edu.unju.fi.logitrack.shipment.entity.Repartidor;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoRepartidor;
import ar.edu.unju.fi.logitrack.shipment.repository.ClienteRepository;
import ar.edu.unju.fi.logitrack.shipment.repository.EnvioRepository;
import ar.edu.unju.fi.logitrack.shipment.repository.RepartidorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración de punta a punta: controller + service + repository + base H2 en memoria.
 * Recorre el flujo completo: registrar repartidor → consultar → asignar a un envío → reglas de negocio.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GestionRepartidoresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepartidorRepository repartidorRepository;

    @Autowired
    private EnvioRepository envioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    private Cliente cliente;

    @BeforeEach
    void limpiarBase() {
        envioRepository.deleteAll();
        repartidorRepository.deleteAll();
        clienteRepository.deleteAll();
        cliente = clienteRepository.save(TestDataFactory.cliente());
    }

    private String registrarRepartidor(String nombre) throws Exception {
        String body = """
                {"nombre": "%s", "tipoVehiculo": "MOTO", "email": "%s@logitrack.test", "telefono": "+54 388 400-2000"}
                """.formatted(nombre, UUID.randomUUID());
        String respuesta = mockMvc.perform(post("/api/v1/repartidores")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    private UUID crearEnvio(String codigo, EstadoEnvio estado) {
        if (estado == EstadoEnvio.PENDIENTE) {
            return envioRepository.save(TestDataFactory.envioPendiente(codigo, cliente)).getId();
        }
        Repartidor repartidor = repartidorRepository.save(TestDataFactory.repartidorActivo());
        Envio envio = TestDataFactory.envioPendiente(codigo, cliente);
        envio.asignarRepartidor(repartidor);
        if (estado == EstadoEnvio.CANCELADO) {
            envio.cancelar();
        } else if (estado == EstadoEnvio.EN_RUTA) {
            envio.cambiarEstado(EstadoEnvio.EN_RUTA);
        } else if (estado == EstadoEnvio.ENTREGADO) {
            envio.cambiarEstado(EstadoEnvio.EN_RUTA);
            envio.cambiarEstado(EstadoEnvio.ENTREGADO);
        }
        return envioRepository.save(envio).getId();
    }

    private String asignacionBody(String repartidorId) {
        return "{\"repartidorId\":\"" + repartidorId + "\"}";
    }

    @Test
    @DisplayName("email único: registrar dos repartidores con el mismo email devuelve 409 y se puede actualizar el contacto")
    void emailUnicoYActualizacion() throws Exception {
        String body = """
                {"nombre": "Ana Torres", "tipoVehiculo": "AUTO", "email": "ana@logitrack.test", "telefono": "+54 388 400-5000"}
                """;
        String creado = mockMvc.perform(post("/api/v1/repartidores").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@logitrack.test"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(creado, "$.id");

        mockMvc.perform(post("/api/v1/repartidores").contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("Ana Torres", "Otra Persona").replace("ana@", "ANA@")))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/v1/repartidores/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("+54 388 400-5000", "+54 388 400-9999")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("+54 388 400-9999"));
    }

    @Test
    @DisplayName("flujo completo: registrar, consultar y asignar un repartidor a un envío PENDIENTE")
    void flujoCompleto() throws Exception {
        String repartidorId = registrarRepartidor("Juan Pérez");
        UUID envioId = crearEnvio("LT-INT-001", EstadoEnvio.PENDIENTE);

        // 1. Consultar detalle del repartidor
        mockMvc.perform(get("/api/v1/repartidores/{id}", repartidorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Juan Pérez"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        // 2. Listar disponibles (?estado=ACTIVO)
        mockMvc.perform(get("/api/v1/repartidores").param("estado", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 3. Asignar repartidor al envío PENDIENTE
        mockMvc.perform(put("/api/v1/envios/{id}/repartidor", envioId)
                        .contentType(MediaType.APPLICATION_JSON).content(asignacionBody(repartidorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoSeguimiento").value("LT-INT-001"))
                .andExpect(jsonPath("$.estadoEnvio").value("PENDIENTE"))
                .andExpect(jsonPath("$.repartidor.id").value(repartidorId));

        // Verificar persistencia en base de datos
        assertThat(envioRepository.existsByRepartidorIdAndEstado(UUID.fromString(repartidorId), EstadoEnvio.PENDIENTE))
                .isTrue();
    }

    @Test
    @DisplayName("un repartidor INACTIVO no puede recibir envíos (422) y deja de figurar como disponible")
    void repartidorInactivoNoSePuedeAsignar() throws Exception {
        String repartidorId = registrarRepartidor("Carlos Gómez");
        UUID envioId = crearEnvio("LT-INT-002", EstadoEnvio.PENDIENTE);

        // Desactivar el repartidor
        mockMvc.perform(patch("/api/v1/repartidores/{id}/estado", repartidorId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));

        // Verificar que ya no figura en los disponibles
        mockMvc.perform(get("/api/v1/repartidores").param("estado", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Intentar asignarlo -> 422 Unprocessable Entity
        mockMvc.perform(put("/api/v1/envios/{id}/repartidor", envioId)
                        .contentType(MediaType.APPLICATION_JSON).content(asignacionBody(repartidorId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("solo se asigna repartidor a envíos PENDIENTES (422 en otros estados)")
    void envioNoPendienteNoSePuedeAsignar() throws Exception {
        String repartidorId = registrarRepartidor("Lucía Fernández");
        UUID enRutaId = crearEnvio("LT-INT-003", EstadoEnvio.EN_RUTA);

        mockMvc.perform(put("/api/v1/envios/{id}/repartidor", enRutaId)
                        .contentType(MediaType.APPLICATION_JSON).content(asignacionBody(repartidorId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("envío o repartidor inexistentes devuelven 404")
    void recursosInexistentes() throws Exception {
        String repartidorId = registrarRepartidor("Martín Quispe");
        UUID envioId = crearEnvio("LT-INT-004", EstadoEnvio.PENDIENTE);

        // Envío inexistente
        mockMvc.perform(put("/api/v1/envios/{id}/repartidor", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(asignacionBody(repartidorId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // Repartidor inexistente
        mockMvc.perform(put("/api/v1/envios/{id}/repartidor", envioId)
                        .contentType(MediaType.APPLICATION_JSON).content(asignacionBody(UUID.randomUUID().toString())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("no se puede desactivar a un repartidor que tiene un envío EN_RUTA")
    void noDesactivarConEnvioEnRuta() throws Exception {
        Repartidor repartidor = repartidorRepository.save(TestDataFactory.repartidorActivo());
        Envio enRuta = TestDataFactory.envioPendiente("LT-INT-005", cliente);
        enRuta.asignarRepartidor(repartidor);
        enRuta.cambiarEstado(EstadoEnvio.EN_RUTA);
        envioRepository.save(enRuta);

        mockMvc.perform(patch("/api/v1/repartidores/{id}/estado", repartidor.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));

        assertThat(repartidorRepository.findById(repartidor.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoRepartidor.ACTIVO);
    }
}
