package dev.andrea.acompaname_backend.payment.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import dev.andrea.acompaname_backend.payment.EstadoPago;

public record PagoDTOResponse(
        Long id,
        BigDecimal importe,
        EstadoPago estado,
        LocalDate fecha,
        Long solicitudId
) {
}