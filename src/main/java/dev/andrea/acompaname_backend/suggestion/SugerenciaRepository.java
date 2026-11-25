package dev.andrea.acompaname_backend.suggestion;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SugerenciaRepository extends JpaRepository<SugerenciaEntity, Long> {

    // Las más recientes primero (si hay empate de fecha, la de id mayor primero)
    List<SugerenciaEntity> findAllByOrderByFechaDescIdDesc();

    // Cuántas se han enviado después de esa fecha
    long countByFechaAfter(LocalDateTime fecha);

    // Borra las sugerencias de una persona (se llama desde un método @Transactional)
    void deleteByUsuarioId(Long usuarioId);
}
