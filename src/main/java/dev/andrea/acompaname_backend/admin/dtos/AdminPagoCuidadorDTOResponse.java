package dev.andrea.acompaname_backend.admin.dtos;

import java.math.BigDecimal;

// Lo que ha cobrado un cuidador (solo cuentan los pagos completados)
public record AdminPagoCuidadorDTOResponse(
        Long cuidadorId,
        String nombre,
        long pagos,
        BigDecimal importeCobrado) {

}