package dev.andrea.acompaname_backend.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.admin.dtos.AvisosDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.CuidadorValoradoDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.EstadisticasDTOResponse;

@RestController
@RequestMapping(path = "${api-endpoint}/admin")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/estadisticas")
    public EstadisticasDTOResponse estadisticas() {
        return service.estadisticas();
    }

    @GetMapping("/avisos")
    public AvisosDTOResponse avisos() {
        return service.avisos();
    }

    @GetMapping("/cuidadores/mejor-valorados")
    public List<CuidadorValoradoDTOResponse> mejoresCuidadores() {
        return service.mejoresCuidadores();
    }
}