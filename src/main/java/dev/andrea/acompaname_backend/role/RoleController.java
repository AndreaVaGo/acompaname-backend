package dev.andrea.acompaname_backend.role;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import dev.andrea.acompaname_backend.role.dtos.RoleDTOResponse;

@RestController
@RequestMapping(path = "${api-endpoint}/roles")
public class RoleController {
    private final RoleService service;

    public RoleController(RoleService service) {
        this.service = service;
    }

    @GetMapping("")
    public List<RoleDTOResponse> index() {
        return service.index();
    }
}