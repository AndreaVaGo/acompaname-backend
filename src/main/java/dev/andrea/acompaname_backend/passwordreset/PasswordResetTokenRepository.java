package dev.andrea.acompaname_backend.passwordreset;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    // Busca por el resumen (hash) del token, nunca por el token de verdad
    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    // Borra todos los tokens de una persona (se llama desde un método @Transactional)
    void deleteByUsuarioId(Long usuarioId);

    // Borra de una vez los caducados (los usa la limpieza nocturna). Devuelve cuántos ha borrado.
    @Transactional
    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity x WHERE x.expiraEn < :ahora")
    int borrarCaducados(@Param("ahora") LocalDateTime ahora);
}
