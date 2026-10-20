package dev.andrea.acompaname_backend.servicerequest;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudRepository extends JpaRepository<SolicitudEntity, Long> {

    // Solicitudes en las que participa el usuario, ya sea como familia o como cuidador
    @Query("SELECT s FROM SolicitudEntity s "
            + "WHERE s.familia.email = :email "
            + "OR s.cuidador.usuario.email = :email")
    List<SolicitudEntity> findByParticipante(@Param("email") String email);

    // Ids de los cuidadores con una solicitud en ese estado cuyas fechas se cruzan con el periodo.
    // Si la solicitud no tiene fecha de fin, cuenta como un solo día (COALESCE).
    @Query("SELECT DISTINCT s.cuidador.id FROM SolicitudEntity s "
            + "WHERE s.estado = :estado "
            + "AND s.fechaCuidado <= :fin "
            + "AND COALESCE(s.fechaFin, s.fechaCuidado) >= :inicio")
    List<Long> findCuidadoresOcupados(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin,
            @Param("estado") EstadoSolicitud estado);

    // Cuántas otras solicitudes de ese cuidador, en ese estado, se cruzan con el periodo.
    // Se excluye la solicitud con ese id (la que se está aceptando, para que no choque consigo misma).
    @Query("SELECT COUNT(s) FROM SolicitudEntity s "
            + "WHERE s.cuidador.id = :cuidadorId "
            + "AND s.id <> :solicitudId "
            + "AND s.estado = :estado "
            + "AND s.fechaCuidado <= :fin "
            + "AND COALESCE(s.fechaFin, s.fechaCuidado) >= :inicio")
    long countSolapamientos(@Param("cuidadorId") Long cuidadorId, @Param("solicitudId") Long solicitudId,
            @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin,
            @Param("estado") EstadoSolicitud estado);

    // Cuántas solicitudes hay en un estado (para las estadísticas del administrador)
    long countByEstado(EstadoSolicitud estado);
}