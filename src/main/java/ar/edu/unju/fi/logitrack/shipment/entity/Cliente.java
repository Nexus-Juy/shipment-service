package ar.edu.unju.fi.logitrack.shipment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

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

    public Cliente(@NonNull String nombre, @NonNull String email, String telefono) {
        this.nombre = nombre;
        this.email = email.toLowerCase();
        this.telefono = telefono;
    }

    public void actualizarContacto(@NonNull String nombre, String telefono) {
        this.nombre = nombre;
        this.telefono = telefono;
    }
}
