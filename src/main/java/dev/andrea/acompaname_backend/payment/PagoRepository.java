package dev.andrea.acompaname_backend.payment;

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

}