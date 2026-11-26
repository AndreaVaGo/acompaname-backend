package dev.andrea.acompaname_backend.schedule;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SuscripcionCalendarioRepository extends JpaRepository<SuscripcionCalendarioEntity, Long> {

    Optional<SuscripcionCalendarioEntity> findByCuidadorId(Long cuidadorId);

    // Se llama desde un método @Transactional (al anonimizar una cuenta)
    void deleteByCuidadorId(Long cuidadorId);
}
