package ar.edu.unju.fi.logitrack.shipment.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Genera códigos de seguimiento legibles, ej. {@code LT-7KQ2M9XHPA}.
 * Se excluyen caracteres ambiguos (0/O, 1/I) para que el cliente no se equivoque al tipearlo.
 */
@Component
public class TrackingCodeGenerator {

    private static final String PREFIJO = "LT-";
    private static final char[] ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int LONGITUD = 10;

    private final SecureRandom random = new SecureRandom();

    public String generar() {
        StringBuilder codigo = new StringBuilder(PREFIJO);
        for (int i = 0; i < LONGITUD; i++) {
            codigo.append(ALFABETO[random.nextInt(ALFABETO.length)]);
        }
        return codigo.toString();
    }
}
