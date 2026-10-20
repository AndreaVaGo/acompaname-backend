package dev.andrea.acompaname_backend.role;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import dev.andrea.acompaname_backend.role.dtos.RoleDTOResponse;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository repository;

    public RoleServiceImpl(RoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RoleDTOResponse> index() {
        List<RoleDTOResponse> respuesta = new ArrayList<>();
        for (RoleEntity rol : repository.findAll()) {
            // El rol ADMIN no se enseña: la lista es pública y sirve para registrarse
            if (!rol.getName().equals("ADMIN")) {
                respuesta.add(new RoleDTOResponse(rol.getId(), rol.getName()));
            }
        }
        return respuesta;
    }
}