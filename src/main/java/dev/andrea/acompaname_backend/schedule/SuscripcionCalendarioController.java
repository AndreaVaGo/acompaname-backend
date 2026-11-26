package dev.andrea.acompaname_backend.schedule;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.schedule.dtos.SuscripcionCalendarioDTOResponse;
import dev.andrea.acompaname_backend.schedule.dtos.VincularCalendarioDTORequest;
import jakarta.validation.Valid;

// Vincular el calendario de la persona cuidadora con uno externo para que se mantenga al día solo
@RestController
@RequestMapping(path = "${api-endpoint}/bloques-ocupados/suscripcion")
public class SuscripcionCalendarioController {

    private final SuscripcionCalendarioService service;

    public SuscripcionCalendarioController(SuscripcionCalendarioService service) {
        this.service = service;
    }

    @GetMapping("")
    public ResponseEntity<SuscripcionCalendarioDTOResponse> estado() {
        return ResponseEntity.ok(service.estado());
    }

    @PutMapping("")
    public ResponseEntity<SuscripcionCalendarioDTOResponse> vincular(
            @Valid @RequestBody VincularCalendarioDTORequest dto) {
        return ResponseEntity.ok(service.vincular(dto.url()));
    }

    @PostMapping("/sincronizar")
    public ResponseEntity<SuscripcionCalendarioDTOResponse> sincronizar() {
        return ResponseEntity.ok(service.sincronizarAhora());
    }

    @DeleteMapping("")
    public ResponseEntity<Void> desvincular() {
        service.desvincular();
        return ResponseEntity.noContent().build();
    }
}
