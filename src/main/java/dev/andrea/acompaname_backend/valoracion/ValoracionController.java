package dev.andrea.acompaname_backend.valoracion;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.valoracion.dtos.ValoracionDTORequest;
import dev.andrea.acompaname_backend.valoracion.dtos.ValoracionDTOResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "${api-endpoint}/valoraciones")
public class ValoracionController {

    private final ValoracionService service;

    public ValoracionController(ValoracionService service) {
        this.service = service;
    }

    @GetMapping("")
    public List<ValoracionEntity> index() {
        return service.getEntities();
    }

    @GetMapping("{id}")
    public ValoracionEntity getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping("")
    public ResponseEntity<ValoracionDTOResponse> store(@Valid @RequestBody ValoracionDTORequest dto) {
        ValoracionDTOResponse dtoResponse = service.storeEntity(dto);
        return ResponseEntity.status(201).body(dtoResponse);
    }
}
