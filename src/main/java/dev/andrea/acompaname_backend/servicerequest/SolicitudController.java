package dev.andrea.acompaname_backend.servicerequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.generics.PaginaDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.dtos.CambiarEstadoDTO;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "${api-endpoint}/solicitudes")
public class SolicitudController {

    private final SolicitudService service;

    public SolicitudController(SolicitudService service) {
        this.service = service;
    }

    @GetMapping("")
    public List<SolicitudDTOResponse> index() {
        return service.getEntities();
    }

    @GetMapping("{id}")
    public SolicitudDTOResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/mis-solicitudes")
    public List<SolicitudDTOResponse> getMisSolicitudes() {
        return service.getMisSolicitudes();
    }

    // Las solicitudes del usuario por páginas (page empieza en 0; size tiene un máximo en el servicio)
    @GetMapping("/mis-solicitudes/pagina")
    public PaginaDTOResponse<SolicitudDTOResponse> getMisSolicitudesPaginadas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getMisSolicitudesPaginadas(page, size);
    }

    // Público: solo devuelve ids de cuidadores ocupados, para filtrar la búsqueda por fechas y horas.
    // La fecha de fin y las horas son opcionales: sin ellas se busca el día completo
    @GetMapping("/cuidadores-ocupados")
    public List<Long> getCuidadoresOcupados(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin) {
        return service.getCuidadoresOcupados(inicio, fin, horaInicio, horaFin);
    }

    @PostMapping("")
    public ResponseEntity<SolicitudDTOResponse> store(@Valid @RequestBody SolicitudDTORequest dto) {
        SolicitudDTOResponse dtoResponse = service.storeEntity(dto);
        return ResponseEntity.status(201).body(dtoResponse);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("{id}")
    public ResponseEntity<SolicitudDTOResponse> update(@PathVariable Long id,
            @Valid @RequestBody SolicitudDTORequest dto) {
        SolicitudDTOResponse dtoResponse = service.update(id, dto);
        return ResponseEntity.status(200).body(dtoResponse);
    }

    @PatchMapping("{id}/estado")
    public ResponseEntity<SolicitudDTOResponse> cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambiarEstadoDTO dto) {
        SolicitudDTOResponse dtoResponse = service.cambiarEstado(id, dto.estado());
        return ResponseEntity.status(200).body(dtoResponse);
    }

}