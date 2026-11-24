package dev.andrea.acompaname_backend.fichaje;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.fichaje.dtos.FichajeDTORequest;
import dev.andrea.acompaname_backend.fichaje.dtos.FichajeDTOResponse;
import jakarta.validation.Valid;

// Fichar el inicio y el fin de un servicio (solo las personas cuidadoras) y ver los fichajes
@RestController
@RequestMapping(path = "${api-endpoint}/fichajes")
public class FichajeController {

    private final FichajeService service;

    public FichajeController(FichajeService service) {
        this.service = service;
    }

    @PostMapping("/{solicitudId}/inicio")
    public ResponseEntity<FichajeDTOResponse> inicio(@PathVariable Long solicitudId,
            @Valid @RequestBody(required = false) FichajeDTORequest dto) {
        return ResponseEntity.status(201).body(service.empezar(solicitudId, ubicacionDe(dto)));
    }

    @PostMapping("/{solicitudId}/fin")
    public ResponseEntity<FichajeDTOResponse> fin(@PathVariable Long solicitudId,
            @Valid @RequestBody(required = false) FichajeDTORequest dto) {
        return ResponseEntity.status(201).body(service.terminar(solicitudId, ubicacionDe(dto)));
    }

    @GetMapping("/{solicitudId}")
    public ResponseEntity<List<FichajeDTOResponse>> index(@PathVariable Long solicitudId) {
        return ResponseEntity.ok(service.listar(solicitudId));
    }

    // Sin cuerpo se ficha igual, como "sin ubicación"
    private FichajeDTORequest ubicacionDe(FichajeDTORequest dto) {
        if (dto == null) {
            return new FichajeDTORequest(null, null);
        }
        return dto;
    }
}
