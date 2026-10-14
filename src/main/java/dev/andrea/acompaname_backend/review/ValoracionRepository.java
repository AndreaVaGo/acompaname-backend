package dev.andrea.acompaname_backend.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ValoracionRepository extends JpaRepository<ValoracionEntity, Long> {

    boolean existsBySolicitudId(Long solicitudId);

    // Media de las puntuaciones de un cuidador (null si todavía no tiene valoraciones)
    @Query("SELECT AVG(v.puntuacion) FROM ValoracionEntity v WHERE v.solicitud.cuidador.id = :cuidadorId")
    Double mediaPorCuidador(@Param("cuidadorId") Long cuidadorId);

    long countBySolicitudCuidadorId(Long cuidadorId);

}