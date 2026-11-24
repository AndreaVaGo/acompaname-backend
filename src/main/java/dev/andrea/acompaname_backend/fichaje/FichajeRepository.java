package dev.andrea.acompaname_backend.fichaje;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FichajeRepository extends JpaRepository<FichajeEntity, Long> {

    // Los fichajes de una solicitud, del más antiguo al más reciente (primero el inicio y luego el fin)
    List<FichajeEntity> findBySolicitudIdOrderByMomentoAsc(Long solicitudId);

    // Para no fichar dos veces lo mismo
    boolean existsBySolicitudIdAndTipo(Long solicitudId, TipoFichaje tipo);
}
