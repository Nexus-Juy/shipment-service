package ar.edu.unju.fi.logitrack.shipment.controller;

import ar.edu.unju.fi.logitrack.shipment.dto.envio.CambiarEstadoRequest;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.EnvioResponse;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.HistorialEstadoResponse;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.RegistrarEnvioRequest;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;
import ar.edu.unju.fi.logitrack.shipment.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST encargado de exponer las operaciones del módulo de envíos bajo /api/v1/envios.
 */
@RestController
@RequestMapping("/api/v1/envios")
@Tag(name = "Envíos", description = "Registro, consulta, cambios de estado e historial de los envíos")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    /**
     * POST /api/v1/envios - Registrar un nuevo envío.
     */
    @PostMapping
    @Operation(summary = "Registrar un envío nuevo (queda PENDIENTE y se le calcula la tarifa)")
    @ApiResponse(responseCode = "201", description = "Envío registrado")
    @ApiResponse(responseCode = "400", description = "Faltan datos o tienen un formato inválido")
    @ApiResponse(responseCode = "404", description = "No existe un cliente con el clienteId indicado")
    @ApiResponse(responseCode = "422", description = "No se indicó el clienteId ni los datos del cliente")
    public ResponseEntity<EnvioResponse> registrarEnvio(@Valid @RequestBody RegistrarEnvioRequest request) {
        EnvioResponse nuevoEnvio = shipmentService.registrarEnvio(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(nuevoEnvio.id())
            .toUri();
        return ResponseEntity.created(location).body(nuevoEnvio);
    }

    /**
     * GET /api/v1/envios/{id} - Consultar un envío por su ID (UUID).
     */
    @GetMapping("/{id}")
    @Operation(summary = "Consultar un envío por su id")
    @ApiResponse(responseCode = "200", description = "Envío encontrado")
    @ApiResponse(responseCode = "404", description = "No existe un envío con ese id")
    public ResponseEntity<EnvioResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(shipmentService.obtenerPorId(id));
    }

    /**
     * GET /api/v1/envios/seguimiento/{codigo} - Consultar un envío por su código público de seguimiento.
     */
    @GetMapping("/seguimiento/{codigo}")
    @Operation(summary = "Consultar un envío por su código de seguimiento")
    @ApiResponse(responseCode = "200", description = "Envío encontrado")
    @ApiResponse(responseCode = "404", description = "No existe un envío con ese código")
    public ResponseEntity<EnvioResponse> obtenerPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(shipmentService.obtenerPorCodigoSeguimiento(codigo));
    }

    /**
     * GET /api/v1/envios?estado=EN_RUTA&repartidorId=... - Listar envíos con filtros opcionales y combinables.
     */
    @GetMapping
    @Operation(summary = "Listar envíos, se puede filtrar por estado y por repartidor asignado")
    @ApiResponse(responseCode = "200", description = "Lista de envíos (vacía si no hay ninguno)")
    @ApiResponse(responseCode = "400", description = "Estado o repartidorId con formato inválido")
    public ResponseEntity<List<EnvioResponse>> listar(
        @Parameter(description = "PENDIENTE, EN_RUTA, ENTREGADO o CANCELADO. Si no se manda, no filtra por estado.")
        @RequestParam(required = false) EstadoEnvio estado,
        @Parameter(description = "Id del repartidor: devuelve solo sus envíos asignados (su hoja de ruta).")
        @RequestParam(required = false) UUID repartidorId) {
        return ResponseEntity.ok(shipmentService.listar(estado, repartidorId));
    }

    /**
     * PATCH /api/v1/envios/{id}/estado - Cambiar el estado de un envío.
     */
    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar el estado de un envío respetando el flujo PENDIENTE → EN_RUTA → ENTREGADO")
    @ApiResponse(responseCode = "200", description = "Estado actualizado y registrado en el historial")
    @ApiResponse(responseCode = "404", description = "No existe un envío con ese id")
    @ApiResponse(responseCode = "422", description = "El cambio de estado no está permitido o el envío no tiene repartidor")
    public ResponseEntity<EnvioResponse> cambiarEstado(
        @PathVariable UUID id,
        @Valid @RequestBody CambiarEstadoRequest request) {
        return ResponseEntity.ok(shipmentService.cambiarEstado(id, request));
    }

    /**
     * POST /api/v1/envios/{id}/cancelacion - Cancelar un envío (solo si está en PENDIENTE).
     */
    @PostMapping("/{id}/cancelacion")
    @Operation(summary = "Cancelar un envío (solo si está PENDIENTE)")
    @ApiResponse(responseCode = "200", description = "Envío cancelado")
    @ApiResponse(responseCode = "404", description = "No existe un envío con ese id")
    @ApiResponse(responseCode = "422", description = "El envío ya no está PENDIENTE")
    public ResponseEntity<EnvioResponse> cancelarEnvio(@PathVariable UUID id) {
        return ResponseEntity.ok(shipmentService.cancelarEnvio(id));
    }

    /**
     * GET /api/v1/envios/{id}/historial - Consultar el historial cronológico de cambios de estado.
     */
    @GetMapping("/{id}/historial")
    @Operation(summary = "Ver el historial de cambios de estado de un envío, del más nuevo al más viejo")
    @ApiResponse(responseCode = "200", description = "Historial del envío")
    @ApiResponse(responseCode = "404", description = "No existe un envío con ese id")
    public ResponseEntity<List<HistorialEstadoResponse>> obtenerHistorial(@PathVariable UUID id) {
        return ResponseEntity.ok(shipmentService.obtenerHistorial(id));
    }
}
