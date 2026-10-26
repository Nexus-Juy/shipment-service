package ar.edu.unju.fi.logitrack.shipment.entity;

import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoRepartidor;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.TipoVehiculo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Personal de entrega. Todo repartidor nuevo inicia ACTIVO; el email es único y sirve de contacto operativo.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "repartidores")
public class Repartidor extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vehiculo", nullable = false, length = 20)
    private TipoVehiculo tipoVehiculo;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 30)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRepartidor estado;

    public Repartidor(@NonNull String nombre, @NonNull TipoVehiculo tipoVehiculo,
                      @NonNull String email, @NonNull String telefono) {
        this.nombre = nombre;
        this.tipoVehiculo = tipoVehiculo;
        this.email = email.toLowerCase();
        this.telefono = telefono;
        this.estado = EstadoRepartidor.ACTIVO;
    }

    /** Reemplaza los datos personales y operativos; el estado se cambia por separado. */
    public void actualizarDatos(@NonNull String nombre, @NonNull TipoVehiculo tipoVehiculo,
                                @NonNull String email, @NonNull String telefono) {
        this.nombre = nombre;
        this.tipoVehiculo = tipoVehiculo;
        this.email = email.toLowerCase();
        this.telefono = telefono;
    }

    public boolean estaActivo() {
        return estado == EstadoRepartidor.ACTIVO;
    }

    public void activar() {
        this.estado = EstadoRepartidor.ACTIVO;
    }

    /** Un repartidor inactivo no puede recibir nuevos envíos. */
    public void desactivar() {
        this.estado = EstadoRepartidor.INACTIVO;
    }

    public void cambiarEstado(@NonNull EstadoRepartidor nuevoEstado) {
        this.estado = nuevoEstado;
    }
}
