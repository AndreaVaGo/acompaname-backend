package dev.andrea.acompaname_backend.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CodigoLoginRepository extends JpaRepository<CodigoLoginEntity, Long> {

    // Busca por el resumen (hash) del desafío, nunca por el desafío de verdad
    Optional<CodigoLoginEntity> findByDesafioHash(String desafioHash);

    // Borra todos los códigos de una persona (se llama desde un método @Transactional)
    void deleteByUsuarioId(Long usuarioId);
}
