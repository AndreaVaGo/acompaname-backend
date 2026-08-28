package dev.andrea.acompaname_backend.solicitud;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "${api-endpoint}/solicitudes")
public class SolicitudController {

    private final SolicitudService service;

    public SolicitudController(SolicitudService service) {
        this.service = service;
    }

    @GetMapping("")
    public List<SolicitudEntity> index() {
        return service.getEntities();
    }

    @GetMapping("{id}")
    public SolicitudEntity getById(@PathVariable Long id) {
        return service.getById(id);
    }

}
