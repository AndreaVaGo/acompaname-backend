package dev.andrea.acompaname_backend.usuario.dtos;

import dev.andrea.acompaname_backend.usuario.Rol;

public record UsuarioDTOResponse(Long id, String nombre, String email, String telefono, Rol rol) {

}
