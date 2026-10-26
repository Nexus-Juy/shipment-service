package ar.edu.unju.fi.logitrack.shipment;

import ar.edu.unju.fi.logitrack.shipment.entity.Cliente;
import ar.edu.unju.fi.logitrack.shipment.entity.Direccion;
import ar.edu.unju.fi.logitrack.shipment.entity.Envio;
import ar.edu.unju.fi.logitrack.shipment.entity.Repartidor;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoRepartidor;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.TipoVehiculo;

import java.math.BigDecimal;
import java.util.UUID;

/** Datos de prueba reutilizables para que los tests no repitan constructores largos. */
public final class TestDataFactory {

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

    public static Repartidor repartidor(String nombre, TipoVehiculo tipoVehiculo, EstadoRepartidor estado) {
        Repartidor repartidor = new Repartidor(
            nombre, tipoVehiculo, UUID.randomUUID() + "@logitrack.test", "+54 388 400-2000");
        if (estado == EstadoRepartidor.INACTIVO) {
            repartidor.desactivar();
        }
        return repartidor;
    }

    public static Repartidor repartidorActivo() {
        return repartidor("Lucía Fernández", TipoVehiculo.MOTO, EstadoRepartidor.ACTIVO);
    }

    public static Repartidor repartidorInactivo() {
        return repartidor("Sofía Mamaní", TipoVehiculo.BICICLETA, EstadoRepartidor.INACTIVO);
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
