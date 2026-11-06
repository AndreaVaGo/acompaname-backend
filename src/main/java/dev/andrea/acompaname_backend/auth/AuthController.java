package dev.andrea.acompaname_backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.security.JwtService;
import dev.andrea.acompaname_backend.security.LimitadorDePeticiones;
import dev.andrea.acompaname_backend.security.SecurityUser;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "${api-endpoint}")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LimitadorDePeticiones limitador;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
            LimitadorDePeticiones limitador) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.limitador = limitador;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDTOResponse> login(@Valid @RequestBody LoginDTORequest request,
            HttpServletRequest peticion) {
        // Antes de comprobar nada, se mira que esta dirección IP no haya hecho demasiados intentos
        limitador.comprobar("login:" + peticion.getRemoteAddr());

        Authentication auth;
        try {
            // Comprueba el email y la contraseña (y que la persona no esté bloqueada)
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException e) {
            // Siempre el mismo 401, sin decir si falló el email o la contraseña
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SecurityUser securityUser = (SecurityUser) auth.getPrincipal();
        UsuarioEntity usuario = securityUser.getUsuario();
        String rol = auth.getAuthorities().iterator().next().getAuthority();
        String token = jwtService.crearToken(usuario);

        AuthDTOResponse respuesta = new AuthDTOResponse(usuario.getId(), "Logged", usuario.getEmail(), rol, token);
        return ResponseEntity.ok().body(respuesta);
    }

}