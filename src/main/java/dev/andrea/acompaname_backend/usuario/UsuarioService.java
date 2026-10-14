package dev.andrea.acompaname_backend.usuario;

import dev.andrea.acompaname_backend.generics.InterfaceGenericService;
import dev.andrea.acompaname_backend.usuario.dtos.CambiarPasswordDTO;
import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTORequest;
import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTOResponse;

public interface UsuarioService extends InterfaceGenericService<UsuarioEntity, UsuarioDTORequest, UsuarioDTOResponse> {

    void cambiarPassword(Long id, CambiarPasswordDTO dto);

}