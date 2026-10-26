package dev.andrea.acompaname_backend.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.admin.dtos.AdminPagoDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AdminPagosResumenDTOResponse;
import dev.andrea.acompaname_backend.payment.EstadoPago;

// Pagos del panel de administración (solo lectura). La ruta /admin/** ya es solo para el administrador.
@RestController
@RequestMapping(path = "${api-endpoint}/admin/pagos")
public class AdminPagosController {
    private final AdminPagosService service;

    public AdminPagosController(AdminPagosService service) {
        this.service = service;
    }

    @GetMapping("/resumen")
    public AdminPagosResumenDTOResponse resumen() {
        return service.resumen();
    }

    // El estado es opcional: sin él salen todos los pagos
    @GetMapping
    public List<AdminPagoDTOResponse> pagos(@RequestParam(required = false) EstadoPago estado) {
        return service.pagos(estado);
    }
}