package dev.andrea.acompaname_backend.admin.dtos;

import java.math.BigDecimal;

// Datos que se enseñan en el panel de administración
public record EstadisticasDTOResponse(
        long totalFamilias,
        long totalCuidadores,
        long cuidadoresDisponibles,
        long solicitudesPendientes,
        long solicitudesAceptadas,
        long solicitudesRechazadas,
        long solicitudesCompletadas,
        long solicitudesHospital,
        long solicitudesDomicilio,
        BigDecimal ingresosCobrados,
        BigDecimal ingresosPendientes,
        Double valoracionMedia,
        Double horasMediasHastaAceptar) {

}