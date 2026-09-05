package dev.andrea.acompaname_backend.valoracion;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    public List<ValoracionDTOResponse> index() {
        return service.getEntities();
    }

    @GetMapping("{id}")
    public ValoracionDTOResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping("")
    public ResponseEntity<ValoracionDTOResponse> store(@Valid @RequestBody ValoracionDTORequest dto) {
        ValoracionDTOResponse dtoResponse = service.storeEntity(dto);
        return ResponseEntity.status(201).body(dtoResponse);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("{id}")
    public ResponseEntity<ValoracionDTOResponse> update(@PathVariable Long id,
            @Valid @RequestBody ValoracionDTORequest dto) {
        ValoracionDTOResponse dtoResponse = service.update(id, dto);
        return ResponseEntity.status(200).body(dtoResponse);
    }
}