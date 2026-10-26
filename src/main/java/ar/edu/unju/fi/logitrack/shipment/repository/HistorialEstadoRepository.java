package ar.edu.unju.fi.logitrack.shipment.repository;

import ar.edu.unju.fi.logitrack.shipment.entity.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Solo lectura en la práctica: los registros se crean por cascada desde {@code Envio}. */
public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, UUID> {

    List<HistorialEstado> findByEnvioIdOrderByFechaHoraDesc(UUID envioId);

}
