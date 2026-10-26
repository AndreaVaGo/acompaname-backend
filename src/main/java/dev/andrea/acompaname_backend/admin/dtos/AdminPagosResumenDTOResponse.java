package dev.andrea.acompaname_backend.admin.dtos;

import java.math.BigDecimal;
import java.util.List;

// Resumen y análisis de los pagos para el panel de administración.
// Los pagos cancelados no cuentan en los importes ni en los porcentajes.
public record AdminPagosResumenDTOResponse(
        BigDecimal totalCobrado,
        BigDecimal totalPendiente,
        long numeroCobrados,
        long numeroPendientes,
        long numeroCancelados,
        // Importe medio de un pago (cobrados y pendientes); 0 si no hay pagos
        BigDecimal importeMedio,
        // Porcentaje de pagos cobrados sobre cobrados y pendientes; null si no hay ninguno
        Double porcentajeCobrado,
        long pendientesMasDeSieteDias,
        BigDecimal importePendienteMasDeSieteDias,
        List<AdminPagoTipoDTOResponse> porTipoCuidado,
        // Los últimos seis meses, del más antiguo al actual
        List<AdminPagoMesDTOResponse> porMes,
        // Los cinco cuidadores que más han cobrado
        List<AdminPagoCuidadorDTOResponse> topCuidadores) {

}