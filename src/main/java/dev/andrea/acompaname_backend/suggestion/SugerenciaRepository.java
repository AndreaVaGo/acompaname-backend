package dev.andrea.acompaname_backend.suggestion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SugerenciaRepository extends JpaRepository<SugerenciaEntity, Long> {

    // Las más recientes primero (si hay empate de fecha, la de id mayor primero)
    List<SugerenciaEntity> findAllByOrderByFechaDescIdDesc();
}
