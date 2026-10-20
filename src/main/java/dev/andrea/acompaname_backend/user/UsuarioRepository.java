package dev.andrea.acompaname_backend.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    
    Optional<UsuarioEntity> findByEmail(String email);

    // Cuántos usuarios tienen un rol (por ejemplo "FAMILIA" o "CUIDADOR")
    long countByRolesName(String nombreRol);
}
