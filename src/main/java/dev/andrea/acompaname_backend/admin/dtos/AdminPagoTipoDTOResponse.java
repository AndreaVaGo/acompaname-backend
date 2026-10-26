package dev.andrea.acompaname_backend.admin.dtos;

import java.math.BigDecimal;

// Cuánto se ha facturado (pagos cobrados y pendientes) en un tipo de cuidado
public record AdminPagoTipoDTOResponse(
        String tipoCuidado,
        long pagos,
        BigDecimal importe) {

}