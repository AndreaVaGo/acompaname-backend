package dev.andrea.acompaname_backend.verificacion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificacionRepository extends JpaRepository<VerificacionEntity, Long> {

    List<VerificacionEntity> findByCuidadorId(Long cuidadorId);

    Optional<VerificacionEntity> findByCuidadorIdAndTipo(Long cuidadorId, TipoVerificacion tipo);

    // Las más antiguas primero, para revisarlas por orden de llegada
    List<VerificacionEntity> findByEstadoOrderBySolicitadaEnAsc(EstadoVerificacion estado);

    List<VerificacionEntity> findAllByOrderBySolicitadaEnAsc();
}
