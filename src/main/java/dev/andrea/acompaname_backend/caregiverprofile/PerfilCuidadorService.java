package dev.andrea.acompaname_backend.caregiverprofile;

import dev.andrea.acompaname_backend.generics.InterfaceGenericService;
import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTOResponse;

public interface PerfilCuidadorService
        extends InterfaceGenericService<PerfilCuidadorEntity, PerfilCuidadorDTORequest, PerfilCuidadorDTOResponse> {

        PerfilCuidadorDTOResponse getMiPerfil();

}
