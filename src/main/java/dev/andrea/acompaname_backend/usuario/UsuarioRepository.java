package dev.andrea.acompaname_backend.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    
    Optional<UsuarioEntity> findByEmail(String email);
}
