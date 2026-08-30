package dev.andrea.acompaname_backend.perfilcuidador;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.perfilcuidador.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.perfilcuidador.dtos.PerfilCuidadorDTOResponse;
import jakarta.validation.Valid;

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

    @PostMapping("")
    public ResponseEntity<PerfilCuidadorDTOResponse> store(@Valid @RequestBody PerfilCuidadorDTORequest dto) {
        PerfilCuidadorDTOResponse dtoResponse = service.storeEntity(dto);
        return ResponseEntity.status(201).body(dtoResponse);

    }
}
