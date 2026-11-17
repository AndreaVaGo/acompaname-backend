package dev.andrea.acompaname_backend.suggestion;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.suggestion.dtos.AdminSugerenciaDTOResponse;

// Cuelga de /admin, así que la seguridad solo deja pasar al administrador
@RestController
@RequestMapping(path = "${api-endpoint}/admin/sugerencias")
public class AdminSugerenciasController {

    private final SugerenciaService service;

    public AdminSugerenciasController(SugerenciaService service) {
        this.service = service;
    }

    @GetMapping("")
    public List<AdminSugerenciaDTOResponse> index() {
        return service.listar();
    }
}
