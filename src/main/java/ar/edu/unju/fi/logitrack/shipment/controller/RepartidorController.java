package ar.edu.unju.fi.logitrack.shipment.controller;

import ar.edu.unju.fi.logitrack.shipment.dto.repartidor.CambioEstadoRepartidorRequest;
import ar.edu.unju.fi.logitrack.shipment.dto.repartidor.RepartidorRequest;
import ar.edu.unju.fi.logitrack.shipment.dto.repartidor.RepartidorResponse;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoRepartidor;
import ar.edu.unju.fi.logitrack.shipment.service.RepartidorService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST encargado de exponer las operaciones del módulo de repartidores bajo la ruta /api/v1/repartidores.
 * <p>
 * Incluye anotaciones de OpenAPI (Swagger) para la documentación interactiva de la API.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/repartidores")
@Tag(name = "Repartidores", description = "Alta y consulta del personal de entrega")
@RequiredArgsConstructor
public class RepartidorController {

    private final RepartidorService repartidorService;

    /**
     * POST /api/v1/repartidores - Registrar un nuevo repartidor en el sistema.
     * <p>
     * Todo repartidor nuevo se crea por defecto en estado ACTIVO.
     * Devuelve el código HTTP 201 Created junto con el encabezado 'Location' apuntando al nuevo recurso.
     * </p>
     *
     * @param request DTO validado con los datos para la alta del repartidor.
     * @return ResponseEntity con el DTO RepartidorResponse y el encabezado Location.
     */
    @PostMapping
    @Operation(summary = "Registrar un repartidor (queda ACTIVO)")
    @ApiResponse(responseCode = "201", description = "Repartidor creado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos o incompletos")
    @ApiResponse(responseCode = "409", description = "Ya existe un repartidor con ese email")
    public ResponseEntity<RepartidorResponse> registrar(@Valid @RequestBody RepartidorRequest request) {
        // Invoca el servicio para procesar el registro
        RepartidorResponse creado = repartidorService.registrar(request);

        // Construcción dinámica de la URI para el encabezado 'Location' (ej. /api/v1/repartidores/{id})
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(creado.id())
            .toUri();

        return ResponseEntity.created(location).body(creado);
    }

    /**
     * GET /api/v1/repartidores - Listar repartidores registrados.
     * <p>
     * Permite filtrar opcionalmente por el parámetro de consulta 'estado' (ACTIVO o INACTIVO).
     * </p>
     *
     * @param estado Estado del repartidor para filtrar la lista (opcional).
     * @return ResponseEntity con la lista de repartidores en formato DTO.
     */
    @GetMapping
    @Operation(summary = "Listar repartidores, opcionalmente filtrando por estado (disponibles con ?estado=ACTIVO)")
    @ApiResponse(responseCode = "200", description = "Lista de repartidores")
    public List<RepartidorResponse> listar(
            @Parameter(description = "Filtro opcional: ACTIVO o INACTIVO") @RequestParam(required = false) EstadoRepartidor estado) {
        return repartidorService.listar(estado);
    }

    /**
     * GET /api/v1/repartidores/{id} - Obtener el detalle de un repartidor específico.
     *
     * @param id Identificador único (UUID) del repartidor a consultar.
     * @return ResponseEntity con los datos del repartidor encontrado.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener el detalle de un repartidor por su ID")
    @ApiResponse(responseCode = "200", description = "Repartidor encontrado")
    @ApiResponse(responseCode = "404", description = "No existe un repartidor con el ID especificado")
    public ResponseEntity<RepartidorResponse> obtener(@PathVariable UUID id) {
        RepartidorResponse repartidor = repartidorService.obtenerPorId(id);
        return ResponseEntity.ok(repartidor);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar los datos de un repartidor (nombre, vehículo, email y teléfono)")
    @ApiResponse(responseCode = "200", description = "Repartidor actualizado")
    @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos o incompletos")
    @ApiResponse(responseCode = "404", description = "No existe el repartidor")
    @ApiResponse(responseCode = "409", description = "El email ya pertenece a otro repartidor")
    public ResponseEntity<RepartidorResponse> actualizar(
            @PathVariable UUID id, @Valid @RequestBody RepartidorRequest request) {
        return ResponseEntity.ok(repartidorService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Activar o desactivar un repartidor",
            description = "Un repartidor INACTIVO no puede recibir envíos. No se puede desactivar con envíos EN_RUTA.")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "400", description = "Estado inválido")
    @ApiResponse(responseCode = "404", description = "No existe el repartidor")
    @ApiResponse(responseCode = "422", description = "El repartidor tiene envíos EN_RUTA")
    public ResponseEntity<RepartidorResponse> cambiarEstado(
            @PathVariable UUID id, @Valid @RequestBody CambioEstadoRepartidorRequest request) {
        return ResponseEntity.ok(repartidorService.cambiarEstado(id, request.estado()));
    }
}
