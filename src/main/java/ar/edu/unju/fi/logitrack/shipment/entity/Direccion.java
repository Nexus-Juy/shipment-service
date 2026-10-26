package ar.edu.unju.fi.logitrack.shipment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Value Object embebido: no tiene identidad propia ni tabla; se guarda dentro de "envios"
 * con prefijo origen_ / destino_. Es inmutable: para cambiar una dirección se crea otra.
 */
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Embeddable
public class Direccion {

    @NonNull
    @Column(nullable = false, length = 120)
    private String calle;

    @NonNull
    @Column(nullable = false, length = 10)
    private String numero;

    @NonNull
    @Column(nullable = false, length = 80)
    private String ciudad;

    @NonNull
    @Column(name = "codigo_postal", nullable = false, length = 10)
    private String codigoPostal;
}
