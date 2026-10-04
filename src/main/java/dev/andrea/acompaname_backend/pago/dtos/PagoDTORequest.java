package dev.andrea.acompaname_backend.pago.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public record PagoDTORequest(
        @NotNull(message = "El importe no puede ser nulo") BigDecimal importe,
        @NotNull(message = "La solicitud no puede ser nula") Long solicitudId
) {
}