package ar.edu.unju.fi.logitrack.shipment;

import ar.edu.unju.fi.logitrack.shipment.entity.Cliente;
import ar.edu.unju.fi.logitrack.shipment.entity.Direccion;
import ar.edu.unju.fi.logitrack.shipment.entity.Envio;

import java.math.BigDecimal;
import java.util.UUID;

/** Datos de prueba reutilizables para que los tests no repitan constructores largos. */
public final class TestDataFactory {

    /**
     * UUID de Lucía Fernández en courier-service (db/init/02-couriers.sql). Los tests de shipment
     * no necesitan la entidad Repartidor: solo guardan y filtran por este identificador.
     */
    public static final UUID REPARTIDOR_ID = UUID.fromString("3f6c1a52-8e0b-4d7a-9c11-0a1b2c3d4e01");

    private TestDataFactory() {
    }

    public static Cliente cliente() {
        return cliente("ventas@tiendanorte.com");
    }

    public static Cliente cliente(String email) {
        return new Cliente("Tienda Norte SRL", email, "+54 388 400-1000");
    }

    public static Direccion origen() {
        return new Direccion("Belgrano", "450", "San Salvador de Jujuy", "4600");
    }

    public static Direccion destino() {
        return new Direccion("Av. Fascio", "1200", "San Salvador de Jujuy", "4600");
    }

    public static Envio envioPendiente(String codigo, Cliente cliente) {
        return envio(codigo, cliente, "2.5", "30", "20", "10");
    }

    public static Envio envio(String codigo, Cliente cliente,
                              String pesoKg, String altoCm, String anchoCm, String largoCm) {
        return new Envio(codigo, cliente, origen(), destino(),
                new BigDecimal(pesoKg), new BigDecimal(altoCm), new BigDecimal(anchoCm), new BigDecimal(largoCm));
    }
}
