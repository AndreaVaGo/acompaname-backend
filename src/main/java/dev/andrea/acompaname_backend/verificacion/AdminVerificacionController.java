package dev.andrea.acompaname_backend.verificacion;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.verificacion.dtos.AdminVerificacionDTOResponse;
import dev.andrea.acompaname_backend.verificacion.dtos.ResolverVerificacionDTORequest;
import jakarta.validation.Valid;

// Cuelga de /admin, así que solo entra el administrador (lo controla SecurityConfiguration)
@RestController
@RequestMapping(path = "${api-endpoint}/admin/verificaciones")
public class AdminVerificacionController {

    private final VerificacionService service;

    public AdminVerificacionController(VerificacionService service) {
        this.service = service;
    }

    // El estado es opcional: sin él salen todas
    @GetMapping("")
    public List<AdminVerificacionDTOResponse> listar(@RequestParam(required = false) EstadoVerificacion estado) {
        return service.listarParaAdmin(estado);
    }

    @PutMapping("/{id}")
    public AdminVerificacionDTOResponse resolver(@PathVariable Long id,
            @Valid @RequestBody ResolverVerificacionDTORequest dto) {
        return service.resolver(id, dto.aprobar(), dto.motivo());
    }
}
