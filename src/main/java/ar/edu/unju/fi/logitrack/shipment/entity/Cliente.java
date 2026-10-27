package ar.edu.unju.fi.logitrack.shipment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "clientes")
public class Cliente extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 30)
    private String telefono;

    /**
     * Id del usuario de {@code security-service} dueño de este cliente (S2-07). Es el {@code sub}
     * del JWT: permite saber si un {@code CLIENTE} puede ver o cancelar un envío. Queda vacío para
     * los clientes cargados por un ADMIN o por los scripts de la base.
     */
    @Column(name = "usuario_id")
    private UUID usuarioId;

    public Cliente(@NonNull String nombre, @NonNull String email, String telefono) {
        this.nombre = nombre;
        this.email = email.toLowerCase();
        this.telefono = telefono;
    }

    public void actualizarContacto(@NonNull String nombre, String telefono) {
        this.nombre = nombre;
        this.telefono = telefono;
    }

    /** Vincula el cliente con el usuario autenticado que lo dio de alta o lo reutilizó. */
    public void vincularUsuario(@NonNull UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    /** Indica si el cliente pertenece al usuario indicado. */
    public boolean esDeUsuario(UUID usuarioId) {
        return usuarioId != null && usuarioId.equals(this.usuarioId);
    }
}
