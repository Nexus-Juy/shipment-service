package ar.edu.unju.fi.logitrack.shipment.entity;

import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.Instant;

/**
 * Registro de auditoría de un cambio de estado. Solo lo crea {@link Envio#cambiarEstado}
 * (constructor de paquete), así ningún cambio de estado queda sin historial.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "historial_estados")
public class HistorialEstado extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "envio_id", nullable = false, updatable = false)
    private Envio envio;

    /** Nulo en el registro inicial (creación del envío). */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20, updatable = false)
    private EstadoEnvio estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20, updatable = false)
    private EstadoEnvio estadoNuevo;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private Instant fechaHora;

    HistorialEstado(@NonNull Envio envio, EstadoEnvio estadoAnterior,
                    @NonNull EstadoEnvio estadoNuevo, @NonNull Instant fechaHora) {
        this.envio = envio;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.fechaHora = fechaHora;
    }
}
