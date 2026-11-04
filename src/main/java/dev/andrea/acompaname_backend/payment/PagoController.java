package dev.andrea.acompaname_backend.payment;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.payment.dtos.PagoDTOResponse;

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

    @PatchMapping("{id}/pagar")
    public ResponseEntity<PagoDTOResponse> marcarComoPagado(@PathVariable Long id) {
        PagoDTOResponse dtoResponse = service.marcarComoPagado(id);
        return ResponseEntity.status(200).body(dtoResponse);
    }
}