package dev.andrea.acompaname_backend.payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<PagoEntity, Long> {

    boolean existsBySolicitudId(Long solicitudId);

    // Pagos de las solicitudes en las que participa el usuario, ya sea como familia o como cuidador
    @Query("SELECT p FROM PagoEntity p "
            + "WHERE p.solicitud.familia.email = :email "
            + "OR p.solicitud.cuidador.usuario.email = :email")
    List<PagoEntity> findByParticipante(@Param("email") String email);

    // Suma de los importes de los pagos en un estado (null si no hay ninguno)
    @Query("SELECT SUM(p.importe) FROM PagoEntity p WHERE p.estado = :estado")
    BigDecimal sumaPorEstado(@Param("estado") EstadoPago estado);

    // Cuántos pagos en un estado se crearon antes de una fecha (por ejemplo, pendientes de hace más de una semana)
    long countByEstadoAndFechaBefore(EstadoPago estado, LocalDate fecha);

    // Todos los pagos, del más reciente al más antiguo (para la tabla del administrador)
    List<PagoEntity> findAllByOrderByFechaDescIdDesc();

    // Los pagos de un estado, del más reciente al más antiguo
    List<PagoEntity> findByEstadoOrderByFechaDescIdDesc(EstadoPago estado);

}