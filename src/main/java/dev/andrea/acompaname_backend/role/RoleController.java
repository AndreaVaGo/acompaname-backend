package dev.andrea.acompaname_backend.role;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import dev.andrea.acompaname_backend.role.dtos.RoleDTOResponse;

@RestController
@RequestMapping(path = "${api-endpoint}/roles")
public class RoleController {
    private final RoleRepository repository;

    public RoleController(RoleRepository repository) {
        this.repository = repository;
    }

    @GetMapping("")
    public List<RoleDTOResponse> index() {
        return repository.findAll().stream()
                .map(r -> new RoleDTOResponse(r.getId(), r.getName()))
                .collect(Collectors.toList());
    }
}
