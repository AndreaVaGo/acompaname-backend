package dev.andrea.acompaname_backend.review;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ValoracionRepository extends JpaRepository<ValoracionEntity, Long> {

    boolean existsBySolicitudId(Long solicitudId);

    // Media de las puntuaciones de un cuidador (null si todavía no tiene valoraciones)
    @Query("SELECT AVG(v.puntuacion) FROM ValoracionEntity v WHERE v.solicitud.cuidador.id = :cuidadorId")
    Double mediaPorCuidador(@Param("cuidadorId") Long cuidadorId);

    long countBySolicitudCuidadorId(Long cuidadorId);

    // Valoraciones de un cuidador, las más recientes primero
    List<ValoracionEntity> findBySolicitudCuidadorIdOrderByFechaDesc(Long cuidadorId);

    // Valoraciones de las solicitudes de una familia (por su email)
    List<ValoracionEntity> findBySolicitudFamiliaEmail(String email);

    // Valoraciones recibidas por un cuidador (por el email de su usuario)
    List<ValoracionEntity> findBySolicitudCuidadorUsuarioEmail(String email);
}