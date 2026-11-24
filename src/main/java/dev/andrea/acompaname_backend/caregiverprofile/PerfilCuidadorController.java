package dev.andrea.acompaname_backend.caregiverprofile;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTOResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "${api-endpoint}/cuidadores")
public class PerfilCuidadorController {

    private final PerfilCuidadorService service;

    public PerfilCuidadorController(PerfilCuidadorService service) {
        this.service = service;
    }

    @GetMapping("")
    public List<PerfilCuidadorDTOResponse> index() {
        return service.getEntities();
    }

    @GetMapping("{id}")
    public PerfilCuidadorDTOResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // La foto como imagen. Es pública (como el perfil) y el navegador la guarda un día en caché;
    // la dirección lleva un ?v= que cambia con la foto, así que al cambiarla se pide de nuevo
    @GetMapping("{id}/foto")
    public ResponseEntity<byte[]> getFoto(@PathVariable Long id) {
        FotoDescargable foto = service.getFoto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipo()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(foto.bytes());
    }

    @PostMapping("")
    public ResponseEntity<PerfilCuidadorDTOResponse> store(@Valid @RequestBody PerfilCuidadorDTORequest dto) {
        PerfilCuidadorDTOResponse dtoResponse = service.storeEntity(dto);
        return ResponseEntity.status(201).body(dtoResponse);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("{id}")
    public ResponseEntity<PerfilCuidadorDTOResponse> update(@PathVariable Long id,
            @Valid @RequestBody PerfilCuidadorDTORequest dto) {
        PerfilCuidadorDTOResponse dtoResponse = service.update(id, dto);
        return ResponseEntity.status(200).body(dtoResponse);
    }

    @GetMapping("/mi-perfil")
    public PerfilCuidadorDTOResponse getMiPerfil() {
        return service.getMiPerfil();
    }
}