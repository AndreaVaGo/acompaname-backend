package dev.andrea.acompaname_backend.schedule;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BloqueOcupadoRepository extends JpaRepository<BloqueOcupadoEntity, Long> {

    // Los bloques de una persona cuidadora, por orden de fecha y hora
    List<BloqueOcupadoEntity> findByCuidadorIdOrderByFechaAscHoraInicioAsc(Long cuidadorId);

    // Los bloques de TODAS las personas cuidadoras que caen entre dos fechas (incluidas).
    // Se usa para saber quién está ocupado en la búsqueda de las familias
    List<BloqueOcupadoEntity> findByFechaBetween(LocalDate desde, LocalDate hasta);

    // Los bloques de una persona cuidadora entre dos fechas (incluidas). Se usa al aceptar una solicitud
    List<BloqueOcupadoEntity> findByCuidadorIdAndFechaBetween(Long cuidadorId, LocalDate desde, LocalDate hasta);
}
