package ar.edu.unju.fi.logitrack.shipment.controller;

import ar.edu.unju.fi.logitrack.shipment.dto.asignacion.AsignacionRequest;
import ar.edu.unju.fi.logitrack.shipment.dto.asignacion.AsignacionResponse;
import ar.edu.unju.fi.logitrack.shipment.service.AsignacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Asignación de repartidor a un envío. El repartidor es un sub-recurso del envío:
 * PUT /api/v1/envios/{envioId}/repartidor
 * <p>
 * La existencia y el estado del repartidor se consultan a courier-service por REST; esta
 * base de datos solo guarda su identificador.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/envios/{envioId}/repartidor")
@Tag(name = "Asignación", description = "Asignación de repartidores a envíos")
@RequiredArgsConstructor
public class AsignacionController {

    private final AsignacionService asignacionService;

    @PutMapping
    @Operation(summary = "Asignar un repartidor a un envío",
            description = "El envío debe estar PENDIENTE y el repartidor ACTIVO. La asignación es previa al paso a EN_RUTA.")
    @ApiResponse(responseCode = "200", description = "Repartidor asignado con éxito")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "No existe el envío o el repartidor")
    @ApiResponse(responseCode = "409", description = "Conflicto de concurrencia al asignar")
    @ApiResponse(responseCode = "422", description = "Envío no PENDIENTE o repartidor INACTIVO")
    @ApiResponse(responseCode = "503", description = "courier-service no está disponible")
    public ResponseEntity<AsignacionResponse> asignar(
            @PathVariable UUID envioId,
            @Valid @RequestBody AsignacionRequest request) {
        return ResponseEntity.ok(asignacionService.asignarRepartidor(envioId, request.repartidorId()));
    }
}
