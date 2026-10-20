package dev.andrea.acompaname_backend.caregiverprofile;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilCuidadorRepository extends JpaRepository<PerfilCuidadorEntity, Long>{

    boolean existsByUsuarioId(Long usuarioId);

    Optional<PerfilCuidadorEntity> findByUsuarioEmail(String email);

}
