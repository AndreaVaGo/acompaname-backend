package dev.andrea.acompaname_backend.admin.dtos;

import java.math.BigDecimal;

// Lo cobrado y lo pendiente de un mes. El mes va como texto «2026-10».
public record AdminPagoMesDTOResponse(
        String mes,
        BigDecimal cobrado,
        BigDecimal pendiente) {

}