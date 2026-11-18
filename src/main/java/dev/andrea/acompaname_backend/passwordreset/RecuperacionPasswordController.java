package dev.andrea.acompaname_backend.passwordreset;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.security.LimitadorDePeticiones;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "${api-endpoint}")
public class RecuperacionPasswordController {

    // Siempre se contesta lo mismo, exista o no el correo, para que nadie pueda
    // averiguar qué correos están registrados
    static final String MENSAJE_OLVIDE =
            "Si el correo está registrado, te hemos enviado un enlace para recuperar tu contraseña";

    private final RecuperacionPasswordService service;
    private final LimitadorDePeticiones limitador;

    public RecuperacionPasswordController(RecuperacionPasswordService service, LimitadorDePeticiones limitador) {
        this.service = service;
        this.limitador = limitador;
    }

    @PostMapping("/olvide-password")
    public ResponseEntity<String> olvidePassword(@Valid @RequestBody OlvidePasswordDTORequest request,
            HttpServletRequest peticion) {
        limitador.comprobar("olvide:" + peticion.getRemoteAddr());

        service.solicitarRestablecimiento(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(MENSAJE_OLVIDE);
    }

    @PostMapping("/restablecer-password")
    public ResponseEntity<Void> restablecerPassword(@Valid @RequestBody RestablecerPasswordDTORequest request,
            HttpServletRequest peticion) {
        limitador.comprobar("restablecer:" + peticion.getRemoteAddr());

        service.restablecer(request.token(), request.passwordNueva());
        return ResponseEntity.noContent().build();
    }
}
