package dev.andrea.acompaname_backend.admin.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import dev.andrea.acompaname_backend.payment.EstadoPago;

// Un pago tal como lo ve el administrador en la tabla de pagos.
// Solo lleva nombres y datos del pago, nada privado de la solicitud.
public record AdminPagoDTOResponse(
        Long id,
        Long solicitudId,
        String nombreFamilia,
        String nombreCuidador,
        String tipoCuidado,
        BigDecimal importe,
        EstadoPago estado,
        LocalDate fecha,
        long diasDesdeCreacion) {

}