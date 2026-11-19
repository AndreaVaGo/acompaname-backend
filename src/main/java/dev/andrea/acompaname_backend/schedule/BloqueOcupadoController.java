package dev.andrea.acompaname_backend.schedule;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.schedule.dtos.BloqueOcupadoDTORequest;
import dev.andrea.acompaname_backend.schedule.dtos.BloqueOcupadoDTOResponse;
import dev.andrea.acompaname_backend.schedule.dtos.ImportacionCalendarioDTOResponse;
import dev.andrea.acompaname_backend.schedule.dtos.ImportarCalendarioDTORequest;
import dev.andrea.acompaname_backend.schedule.dtos.RepeticionBloquesDTOResponse;
import dev.andrea.acompaname_backend.schedule.dtos.RepetirBloquesDTORequest;
import jakarta.validation.Valid;

// El calendario propio de la persona cuidadora: los ratos en los que no puede trabajar
@RestController
@RequestMapping(path = "${api-endpoint}/bloques-ocupados")
public class BloqueOcupadoController {

    private final BloqueOcupadoService service;

    public BloqueOcupadoController(BloqueOcupadoService service) {
        this.service = service;
    }

    @GetMapping("")
    public ResponseEntity<List<BloqueOcupadoDTOResponse>> index() {
        return ResponseEntity.ok(service.listarMios());
    }

    @PostMapping("")
    public ResponseEntity<BloqueOcupadoDTOResponse> store(@Valid @RequestBody BloqueOcupadoDTORequest dto) {
        return ResponseEntity.status(201).body(service.crear(dto));
    }

    // Importa los ratos ocupados de un calendario externo (archivo .ics)
    @PostMapping("/importar")
    public ResponseEntity<ImportacionCalendarioDTOResponse> importar(
            @Valid @RequestBody ImportarCalendarioDTORequest dto) {
        return ResponseEntity.ok(service.importar(dto.contenido()));
    }

    // Repite un rato ocupado varios días de la semana entre dos fechas (turnos)
    @PostMapping("/repetir")
    public ResponseEntity<RepeticionBloquesDTOResponse> repetir(@Valid @RequestBody RepetirBloquesDTORequest dto) {
        return ResponseEntity.status(201).body(service.repetir(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> destroy(@PathVariable Long id) {
        service.borrar(id);
        return ResponseEntity.noContent().build();
    }
}
