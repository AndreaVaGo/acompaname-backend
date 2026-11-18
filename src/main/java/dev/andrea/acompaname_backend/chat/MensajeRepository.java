package dev.andrea.acompaname_backend.chat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MensajeRepository extends JpaRepository<MensajeEntity, Long> {

    // Todos los mensajes de una solicitud, del más antiguo al más reciente
    List<MensajeEntity> findBySolicitudIdOrderByIdAsc(Long solicitudId);

    // Solo los mensajes nuevos: los que tienen un id mayor que el último que ya se ha visto
    List<MensajeEntity> findBySolicitudIdAndIdGreaterThanOrderByIdAsc(Long solicitudId, Long desdeId);

    // Los mensajes de una solicitud que no ha escrito esa persona y que todavía no ha leído
    List<MensajeEntity> findBySolicitudIdAndEmisorIdNotAndLeidoFalse(Long solicitudId, Long usuarioId);

    // Todos los mensajes sin leer de una persona (en las solicitudes donde es la familia o la persona cuidadora)
    @Query("select m from MensajeEntity m where m.leido = false and m.emisor.email <> :email "
            + "and (m.solicitud.familia.email = :email or m.solicitud.cuidador.usuario.email = :email)")
    List<MensajeEntity> findNoLeidosDe(@Param("email") String email);
}
