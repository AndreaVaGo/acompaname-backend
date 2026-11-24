package dev.andrea.acompaname_backend.servicerequest;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudRepository extends JpaRepository<SolicitudEntity, Long> {

    // Solicitudes en las que participa el usuario, ya sea como familia o como cuidador
    @Query("SELECT s FROM SolicitudEntity s "
            + "WHERE s.familia.email = :email "
            + "OR s.cuidador.usuario.email = :email")
    List<SolicitudEntity> findByParticipante(@Param("email") String email);

    // Lo mismo, pero por páginas: la base de datos ordena y devuelve solo la página pedida (ACOM-234)
    @Query(value = "SELECT s FROM SolicitudEntity s "
            + "WHERE s.familia.email = :email "
            + "OR s.cuidador.usuario.email = :email",
            countQuery = "SELECT COUNT(s) FROM SolicitudEntity s "
                    + "WHERE s.familia.email = :email "
                    + "OR s.cuidador.usuario.email = :email")
    Page<SolicitudEntity> findByParticipante(@Param("email") String email, Pageable pageable);

    // Solicitudes en ese estado cuyos días se cruzan con el periodo (de cualquier cuidador).
    // Si la solicitud no tiene fecha de fin, cuenta como un solo día (COALESCE).
    // Aquí solo se comparan los días: si también se cruzan las horas lo decide SolicitudEntity.seCruzaCon
    @Query("SELECT s FROM SolicitudEntity s "
            + "WHERE s.estado = :estado "
            + "AND s.fechaCuidado <= :fin "
            + "AND COALESCE(s.fechaFin, s.fechaCuidado) >= :inicio")
    List<SolicitudEntity> findPorFechasQueSeCruzan(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin,
            @Param("estado") EstadoSolicitud estado);

    // Otras solicitudes de ese cuidador, en ese estado, cuyos días se cruzan con el periodo.
    // Se excluye la solicitud con ese id (la que se está aceptando, para que no choque consigo misma).
    @Query("SELECT s FROM SolicitudEntity s "
            + "WHERE s.cuidador.id = :cuidadorId "
            + "AND s.id <> :solicitudId "
            + "AND s.estado = :estado "
            + "AND s.fechaCuidado <= :fin "
            + "AND COALESCE(s.fechaFin, s.fechaCuidado) >= :inicio")
    List<SolicitudEntity> findDelCuidadorQueSeCruzan(@Param("cuidadorId") Long cuidadorId,
            @Param("solicitudId") Long solicitudId, @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin, @Param("estado") EstadoSolicitud estado);

    // Cuántas solicitudes hay en un estado (para las estadísticas del administrador)
    long countByEstado(EstadoSolicitud estado);

    // Cuántas solicitudes hay de un tipo de cuidado ("hospitalario" o "domicilio")
    long countByTipoCuidado(String tipoCuidado);

    // Solicitudes que ya tienen fecha de aceptación (para calcular el tiempo medio hasta aceptar)
    List<SolicitudEntity> findByFechaAceptacionIsNotNull();

    // Cuántas solicitudes en un estado tienen la fecha del cuidado entre dos fechas (incluidas)
    long countByEstadoAndFechaCuidadoBetween(EstadoSolicitud estado, LocalDate desde, LocalDate hasta);

    // Cuántas solicitudes en un estado tienen la fecha del cuidado anterior a una fecha
    long countByEstadoAndFechaCuidadoBefore(EstadoSolicitud estado, LocalDate fecha);

    // Cuántas solicitudes de un cuidador están en un estado (por ejemplo, los servicios completados)
    long countByCuidadorIdAndEstado(Long cuidadorId, EstadoSolicitud estado);

    // Solicitudes en un estado (para el listado del administrador)
    List<SolicitudEntity> findByEstado(EstadoSolicitud estado);
}