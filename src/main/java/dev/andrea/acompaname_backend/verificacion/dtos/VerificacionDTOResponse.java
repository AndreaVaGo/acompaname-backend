package dev.andrea.acompaname_backend.verificacion.dtos;

import java.time.LocalDateTime;

import dev.andrea.acompaname_backend.verificacion.EstadoVerificacion;
import dev.andrea.acompaname_backend.verificacion.TipoVerificacion;

// Lo que ve la persona cuidadora de sus propias verificaciones
public record VerificacionDTOResponse(Long id, TipoVerificacion tipo, EstadoVerificacion estado, String referencia,
        LocalDateTime solicitadaEn, LocalDateTime revisadaEn, String motivoRechazo) {

}
