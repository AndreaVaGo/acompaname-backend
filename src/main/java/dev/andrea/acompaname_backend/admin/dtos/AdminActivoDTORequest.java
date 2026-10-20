package dev.andrea.acompaname_backend.admin.dtos;

import jakarta.validation.constraints.NotNull;

// Cuerpo de la petición para bloquear (false) o desbloquear (true) a un usuario
public record AdminActivoDTORequest(

        @NotNull(message = "El campo activo no puede ser nulo") Boolean activo) {

}