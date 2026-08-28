package dev.andrea.acompaname_backend.usuario;

import org.springframework.data.jpa.repository.JpaRepository;


public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    
}
