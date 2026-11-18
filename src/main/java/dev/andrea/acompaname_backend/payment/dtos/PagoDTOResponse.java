package dev.andrea.acompaname_backend.payment.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import dev.andrea.acompaname_backend.payment.EstadoPago;

// horas y personas son los datos con los que se calculó el importe (para enseñar el desglose)
public record PagoDTOResponse(
        Long id,
        BigDecimal importe,
        EstadoPago estado,
        LocalDate fecha,
        Long solicitudId,
        String cuidadorNombre,
        String tipoCuidado,
        Long horas,
        Integer personas
) {
    // Constructor sin horas ni personas, para no romper el código que ya existía
    public PagoDTOResponse(Long id, BigDecimal importe, EstadoPago estado, LocalDate fecha, Long solicitudId,
            String cuidadorNombre, String tipoCuidado) {
        this(id, importe, estado, fecha, solicitudId, cuidadorNombre, tipoCuidado, null, null);
    }
}
