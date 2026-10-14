package dev.andrea.acompaname_backend.user;

import dev.andrea.acompaname_backend.generics.InterfaceGenericService;
import dev.andrea.acompaname_backend.user.dtos.ActualizarContactoDTO;
import dev.andrea.acompaname_backend.user.dtos.CambiarPasswordDTO;
import dev.andrea.acompaname_backend.user.dtos.UsuarioDTORequest;
import dev.andrea.acompaname_backend.user.dtos.UsuarioDTOResponse;

public interface UsuarioService extends InterfaceGenericService<UsuarioEntity, UsuarioDTORequest, UsuarioDTOResponse> {

    void cambiarPassword(Long id, CambiarPasswordDTO dto);

    UsuarioDTOResponse actualizarContacto(Long id, ActualizarContactoDTO dto);

}