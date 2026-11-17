package dev.andrea.acompaname_backend.suggestion;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.security.LimitadorDePeticiones;
import dev.andrea.acompaname_backend.suggestion.dtos.SugerenciaDTORequest;
import dev.andrea.acompaname_backend.suggestion.dtos.SugerenciaDTOResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "${api-endpoint}/sugerencias")
public class SugerenciaController {

    private final SugerenciaService service;
    private final LimitadorDePeticiones limitador;

    public SugerenciaController(SugerenciaService service, LimitadorDePeticiones limitador) {
        this.service = service;
        this.limitador = limitador;
    }

    @PostMapping("")
    public ResponseEntity<SugerenciaDTOResponse> store(@Valid @RequestBody SugerenciaDTORequest dto,
            HttpServletRequest peticion) {
        // Para que nadie llene la base de datos de mensajes: si se pasa del límite, da 429
        limitador.comprobar("sugerencia:" + peticion.getRemoteAddr());
        SugerenciaDTOResponse dtoResponse = service.crear(dto);
        return ResponseEntity.status(201).body(dtoResponse);
    }
}
