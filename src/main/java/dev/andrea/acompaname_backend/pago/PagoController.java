package dev.andrea.acompaname_backend.pago;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.pago.dtos.PagoDTORequest;
import dev.andrea.acompaname_backend.pago.dtos.PagoDTOResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "${api-endpoint}/pagos")
public class PagoController {

    private final PagoService service;

    public PagoController(PagoService service) {
        this.service = service;
    }

    @GetMapping("")
    public List<PagoDTOResponse> index() {
        return service.getEntities();
    }

    @GetMapping("{id}")
    public PagoDTOResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping("")
    public ResponseEntity<PagoDTOResponse> store(@Valid @RequestBody PagoDTORequest dto) {
        PagoDTOResponse dtoResponse = service.storeEntity(dto);
        return ResponseEntity.status(201).body(dtoResponse);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("{id}")
    public ResponseEntity<PagoDTOResponse> update(@PathVariable Long id, @Valid @RequestBody PagoDTORequest dto) {
        PagoDTOResponse dtoResponse = service.update(id, dto);
        return ResponseEntity.status(200).body(dtoResponse);
    }

    @PatchMapping("{id}/pagar")
    public ResponseEntity<PagoDTOResponse> marcarComoPagado(@PathVariable Long id) {
        PagoDTOResponse dtoResponse = service.marcarComoPagado(id);
        return ResponseEntity.status(200).body(dtoResponse);
    }
}