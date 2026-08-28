package dev.andrea.acompaname_backend.perfilcuidador;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "${api-endpoint}/cuidadores")
public class PerfilCuidadorController {

    private final PerfilCuidadorService service;

    public PerfilCuidadorController(PerfilCuidadorService service) {
        this.service = service;
    }
    
    @GetMapping("")
    public List<PerfilCuidadorEntity> index() {
        return service.getEntities();
    }
   
    @GetMapping("{id}")
    public PerfilCuidadorEntity getById(@PathVariable Long id) {
        return service.getById(id);
    }
}

