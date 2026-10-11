package dev.andrea.acompaname_backend.role;

import java.util.List;

import dev.andrea.acompaname_backend.role.dtos.RoleDTOResponse;

public interface RoleService {

    List<RoleDTOResponse> index();
}