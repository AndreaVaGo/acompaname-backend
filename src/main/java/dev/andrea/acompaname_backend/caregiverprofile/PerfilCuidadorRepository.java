package dev.andrea.acompaname_backend.caregiverprofile;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PerfilCuidadorRepository extends JpaRepository<PerfilCuidadorEntity, Long>{

    boolean existsByUsuarioId(Long usuarioId);

    Optional<PerfilCuidadorEntity> findByUsuarioEmail(String email);

    // Todos los cuidadores con su usuario Y los roles del usuario cargados en la misma consulta.
    // Sin los JOIN FETCH, Hibernate haría consultas extra por cada cuidador (problema N+1):
    // una para el usuario (es LAZY) y otra para sus roles (son EAGER)
    @Query("SELECT p FROM PerfilCuidadorEntity p JOIN FETCH p.usuario u LEFT JOIN FETCH u.roles")
    List<PerfilCuidadorEntity> findAllConUsuario();

    // Cuántos cuidadores están disponibles ahora
    long countByDisponibleAhoraTrue();

}
