package dev.andrea.acompaname_backend.passwordreset;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    // Busca por el resumen (hash) del token, nunca por el token de verdad
    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    // Borra todos los tokens de una persona (se llama desde un método @Transactional)
    void deleteByUsuarioId(Long usuarioId);
}
