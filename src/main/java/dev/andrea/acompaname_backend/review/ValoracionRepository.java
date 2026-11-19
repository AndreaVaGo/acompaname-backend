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

    // Media y número de valoraciones de TODOS los cuidadores en una sola consulta.
    // Cada fila es: [id del cuidador, media, número de valoraciones]
    // (los cuidadores sin valoraciones no salen)
    @Query("SELECT v.solicitud.cuidador.id, AVG(v.puntuacion), COUNT(v) FROM ValoracionEntity v "
            + "GROUP BY v.solicitud.cuidador.id")
    List<Object[]> resumenPorCuidador();

    // Valoraciones de un cuidador, las más recientes primero
    List<ValoracionEntity> findBySolicitudCuidadorIdOrderByFechaDesc(Long cuidadorId);

    // Valoraciones de las solicitudes de una familia (por su email)
    List<ValoracionEntity> findBySolicitudFamiliaEmail(String email);

    // Valoraciones recibidas por un cuidador (por el email de su usuario)
    List<ValoracionEntity> findBySolicitudCuidadorUsuarioEmail(String email);

    // Media de todas las puntuaciones de la plataforma (null si no hay ninguna)
    @Query("SELECT AVG(v.puntuacion) FROM ValoracionEntity v")
    Double mediaGeneral();

    // Las cinco valoraciones más recientes (si hay empate de fecha, la de id mayor primero)
    List<ValoracionEntity> findTop5ByOrderByFechaDescIdDesc();
}