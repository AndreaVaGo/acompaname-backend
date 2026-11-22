package dev.andrea.acompaname_backend.verificacion.dtos;

import java.time.LocalDateTime;

import dev.andrea.acompaname_backend.verificacion.EstadoVerificacion;
import dev.andrea.acompaname_backend.verificacion.TipoVerificacion;

// Lo que ve el administrador para revisar una verificación
public record AdminVerificacionDTOResponse(Long id, Long cuidadorId, String cuidadorNombre, TipoVerificacion tipo,
        EstadoVerificacion estado, String referencia, LocalDateTime solicitadaEn, LocalDateTime revisadaEn,
        String motivoRechazo) {

}
