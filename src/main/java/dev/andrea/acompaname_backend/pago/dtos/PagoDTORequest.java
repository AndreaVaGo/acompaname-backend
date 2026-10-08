package dev.andrea.acompaname_backend.pago.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PagoDTORequest(
        @NotNull(message = "El importe no puede ser nulo") @Positive(message = "El importe debe ser mayor que 0") BigDecimal importe,
        @NotNull(message = "La solicitud no puede ser nula") Long solicitudId
) {
}