package dev.andrea.acompaname_backend.pago.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import dev.andrea.acompaname_backend.pago.EstadoPago;

public record PagoDTOResponse(
        Long id,
        BigDecimal importe,
        EstadoPago estado,
        LocalDate fecha,
        Long solicitudId
) {
}