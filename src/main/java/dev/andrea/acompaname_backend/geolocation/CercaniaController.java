package dev.andrea.acompaname_backend.geolocation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.geolocation.dtos.CercaniaDTOResponse;

// Para ordenar la búsqueda de cuidadores por cercanía. Es público, como el listado de cuidadores
@RestController
@RequestMapping(path = "${api-endpoint}/cuidadores/cerca")
public class CercaniaController {

    private final CercaniaService service;

    public CercaniaController(CercaniaService service) {
        this.service = service;
    }

    @GetMapping("")
    public CercaniaDTOResponse index(@RequestParam(defaultValue = "") String lugar) {
        return service.buscarCerca(lugar);
    }
}
