package dev.andrea.acompaname_backend.caregiverprofile;

import dev.andrea.acompaname_backend.generics.InterfaceGenericService;
import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTOResponse;

public interface PerfilCuidadorService
        extends InterfaceGenericService<PerfilCuidadorEntity, PerfilCuidadorDTORequest, PerfilCuidadorDTOResponse> {

        PerfilCuidadorDTOResponse getMiPerfil();

        // La foto del perfil como imagen (para pedirla aparte y que el navegador la guarde en caché).
        // Si el perfil no existe o no tiene foto, lanza el error de "no encontrado"
        FotoDescargable getFoto(Long id);

}
