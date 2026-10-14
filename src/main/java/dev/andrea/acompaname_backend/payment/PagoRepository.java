package dev.andrea.acompaname_backend.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<PagoEntity, Long> {

    boolean existsBySolicitudId(Long solicitudId);

}