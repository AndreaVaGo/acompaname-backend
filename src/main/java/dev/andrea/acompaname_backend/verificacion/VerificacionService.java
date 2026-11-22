package dev.andrea.acompaname_backend.verificacion;

import java.util.List;

import dev.andrea.acompaname_backend.verificacion.dtos.AdminVerificacionDTOResponse;
import dev.andrea.acompaname_backend.verificacion.dtos.VerificacionDTOResponse;
import dev.andrea.acompaname_backend.verificacion.dtos.VerificadosDTOResponse;

public interface VerificacionService {

    // Las verificaciones de la persona cuidadora que ha iniciado sesión
    List<VerificacionDTOResponse> listarMias();

    // Envía (o vuelve a enviar) una referencia para revisar
    VerificacionDTOResponse enviar(TipoVerificacion tipo, String referencia);

    // Quién tiene algo verificado (público, sin datos sensibles)
    List<VerificadosDTOResponse> publicas();

    // Para el administrador: todas o solo las de un estado
    List<AdminVerificacionDTOResponse> listarParaAdmin(EstadoVerificacion estado);

    // El administrador aprueba o rechaza una verificación pendiente
    AdminVerificacionDTOResponse resolver(Long id, boolean aprobar, String motivo);
}
