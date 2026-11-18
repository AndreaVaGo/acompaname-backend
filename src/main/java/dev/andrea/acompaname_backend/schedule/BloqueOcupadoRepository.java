package dev.andrea.acompaname_backend.schedule;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BloqueOcupadoRepository extends JpaRepository<BloqueOcupadoEntity, Long> {

    // Los bloques de una persona cuidadora, por orden de fecha y hora
    List<BloqueOcupadoEntity> findByCuidadorIdOrderByFechaAscHoraInicioAsc(Long cuidadorId);
}
