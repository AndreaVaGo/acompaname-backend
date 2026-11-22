package dev.andrea.acompaname_backend.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.security.LimitadorDePeticiones;
import dev.andrea.acompaname_backend.security.SecurityUser;
import jakarta.validation.Valid;

// Cada persona ve y cambia SU segundo paso: el id sale del token, nunca de la URL
@RestController
@RequestMapping(path = "${api-endpoint}/dos-pasos")
public class DosPasosController {

    private final DosPasosService service;
    private final LimitadorDePeticiones limitador;

    public DosPasosController(DosPasosService service, LimitadorDePeticiones limitador) {
        this.service = service;
        this.limitador = limitador;
    }

    @GetMapping("")
    public DosPasosEstadoDTOResponse estado(Authentication authentication) {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        return new DosPasosEstadoDTOResponse(securityUser.getUsuario().isDosPasos());
    }

    @PutMapping("")
    public ResponseEntity<DosPasosEstadoDTOResponse> cambiar(@Valid @RequestBody CambiarDosPasosDTORequest dto,
            Authentication authentication) {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        Long usuarioId = securityUser.getUsuario().getId();
        // Se pide la contraseña, así que se limitan los intentos para que no se pueda probar sin fin
        limitador.comprobar("dos-pasos:" + usuarioId);
        service.cambiarEstado(usuarioId, dto.activo(), dto.password());
        return ResponseEntity.ok(new DosPasosEstadoDTOResponse(dto.activo()));
    }
}
