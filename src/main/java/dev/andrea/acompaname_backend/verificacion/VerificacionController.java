package dev.andrea.acompaname_backend.verificacion;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.verificacion.dtos.EnviarVerificacionDTORequest;
import dev.andrea.acompaname_backend.verificacion.dtos.VerificacionDTOResponse;
import dev.andrea.acompaname_backend.verificacion.dtos.VerificadosDTOResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "${api-endpoint}/verificaciones")
public class VerificacionController {

    private final VerificacionService service;

    public VerificacionController(VerificacionService service) {
        this.service = service;
    }

    // Público: qué cuidadores tienen titulación o antecedentes verificados (solo "sí/no")
    @GetMapping("/publicas")
    public List<VerificadosDTOResponse> publicas() {
        return service.publicas();
    }

    // Solo personas cuidadoras: sus propias verificaciones
    @GetMapping("/mias")
    public List<VerificacionDTOResponse> mias() {
        return service.listarMias();
    }

    // Solo personas cuidadoras: enviar (o reenviar) la referencia de una prueba
    @PutMapping("/{tipo}")
    public VerificacionDTOResponse enviar(@PathVariable TipoVerificacion tipo,
            @Valid @RequestBody EnviarVerificacionDTORequest dto) {
        return service.enviar(tipo, dto.referencia());
    }
}
