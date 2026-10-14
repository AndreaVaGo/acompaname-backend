package dev.andrea.acompaname_backend.user.dtos;

import java.util.Set;

public record UsuarioDTOResponse(Long id, String nombre, String email, String telefono, Set<String> roles) {



    
}