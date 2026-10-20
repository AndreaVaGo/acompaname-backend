package dev.andrea.acompaname_backend.servicerequest;

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
}