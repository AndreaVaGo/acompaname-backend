package dev.andrea.acompaname_backend.admin.dtos;

import java.time.LocalDate;

import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;

// Una solicitud tal como la ve el administrador en el panel.
// No incluye la dirección ni las notas del paciente, porque son datos privados.
public record AdminSolicitudDTOResponse(
        Long id,
        String tipoCuidado,
        String nombrePaciente,
        LocalDate fechaCuidado,
        EstadoSolicitud estado,
        String nombreFamilia,
        String nombreCuidador) {

}