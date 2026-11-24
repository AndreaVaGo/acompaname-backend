package dev.andrea.acompaname_backend.user;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    
    Optional<UsuarioEntity> findByEmail(String email);

    // Cuántos usuarios tienen un rol (por ejemplo "FAMILIA" o "CUIDADOR")
    long countByRolesName(String nombreRol);

    // Cuántas cuentas se han creado después de esa fecha (las antiguas, sin fecha de alta, no cuentan)
    long countByFechaAltaAfter(LocalDateTime fecha);
}
