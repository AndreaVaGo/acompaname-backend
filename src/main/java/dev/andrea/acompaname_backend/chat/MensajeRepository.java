package dev.andrea.acompaname_backend.chat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MensajeRepository extends JpaRepository<MensajeEntity, Long> {

    // Todos los mensajes de una solicitud, del más antiguo al más reciente
    List<MensajeEntity> findBySolicitudIdOrderByIdAsc(Long solicitudId);

    // Solo los mensajes nuevos: los que tienen un id mayor que el último que ya se ha visto
    List<MensajeEntity> findBySolicitudIdAndIdGreaterThanOrderByIdAsc(Long solicitudId, Long desdeId);
}
