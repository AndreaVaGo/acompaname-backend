package dev.andrea.acompaname_backend.admin.dtos;

// Avisos del panel de administración: cosas que el administrador debería revisar
public record AvisosDTOResponse(
        long pendientesProximas48Horas,
        long pendientesVencidas,
        long pagosSinCobrarMasDeUnaSemana) {

}