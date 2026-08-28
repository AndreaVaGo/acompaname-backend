package dev.andrea.acompaname_backend.usuario;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(path = "${api-endpoint}/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping("")
    public List<UsuarioEntity> index() {
        return service.getEntities();
    }

    @GetMapping("{id}")
    public UsuarioEntity getById(@PathVariable Long id) {
        return service.getById(id);
    }
    
    
    
}
