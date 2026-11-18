package dev.andrea.acompaname_backend.chat;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.chat.dtos.MensajeDTORequest;
import dev.andrea.acompaname_backend.chat.dtos.MensajeDTOResponse;
import jakarta.validation.Valid;

// El chat de cada solicitud. El front pregunta cada pocos segundos con "desde" (el último id que ya tiene)
@RestController
@RequestMapping(path = "${api-endpoint}/solicitudes/{solicitudId}/mensajes")
public class MensajeController {

    private final MensajeService service;

    public MensajeController(MensajeService service) {
        this.service = service;
    }

    @GetMapping("")
    public List<MensajeDTOResponse> index(@PathVariable Long solicitudId,
            @RequestParam(required = false) Long desde) {
        return service.getMensajes(solicitudId, desde);
    }

    @PostMapping("")
    public ResponseEntity<MensajeDTOResponse> store(@PathVariable Long solicitudId,
            @Valid @RequestBody MensajeDTORequest dto) {
        return ResponseEntity.status(201).body(service.enviar(solicitudId, dto));
    }
}
