package dev.andrea.acompaname_backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import dev.andrea.acompaname_backend.captcha.VerificadorCaptcha;
import dev.andrea.acompaname_backend.security.JwtService;
import dev.andrea.acompaname_backend.security.LimitadorDePeticiones;
import dev.andrea.acompaname_backend.security.SecurityUser;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "${api-endpoint}")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LimitadorDePeticiones limitador;
    private final VerificadorCaptcha verificadorCaptcha;
    private final DosPasosService dosPasosService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
            LimitadorDePeticiones limitador, VerificadorCaptcha verificadorCaptcha,
            DosPasosService dosPasosService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.limitador = limitador;
        this.verificadorCaptcha = verificadorCaptcha;
        this.dosPasosService = dosPasosService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTORequest request,
            HttpServletRequest peticion) {
        // Antes de comprobar nada, se mira que esta dirección IP no haya hecho demasiados intentos
        limitador.comprobar("login:" + peticion.getRemoteAddr());
        // Después, el CAPTCHA (si está activado). Sin él válido, ni se mira la contraseña
        verificadorCaptcha.verificar(request.captchaToken(), peticion.getRemoteAddr());

        Authentication auth;
        try {
            // Comprueba el email y la contraseña (y que la persona no esté bloqueada)
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException e) {
            // Se deja un aviso en el log solo con la IP (la hora la pone el propio log), para poder ver
            // ataques de fuerza bruta. Nunca se escribe el email ni la contraseña
            log.warn("Intento de login fallido desde la IP {}", peticion.getRemoteAddr());
            // Siempre el mismo 401, sin decir si falló el email o la contraseña
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SecurityUser securityUser = (SecurityUser) auth.getPrincipal();
        UsuarioEntity usuario = securityUser.getUsuario();

        // Si tiene el segundo paso activado, todavía no se le da el token: se le manda un código por correo
        // y se responde 202 con el "desafío" que tendrá que enviar a /login/verificar junto con el código
        if (usuario.isDosPasos()) {
            String desafio = dosPasosService.iniciar(usuario);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(new DosPasosDTOResponse(true, desafio));
        }

        String rol = auth.getAuthorities().iterator().next().getAuthority();
        String token = jwtService.crearToken(usuario);

        AuthDTOResponse respuesta = new AuthDTOResponse(usuario.getId(), "Logged", usuario.getEmail(), rol, token);
        return ResponseEntity.ok().body(respuesta);
    }

    // Segundo paso del login: se comprueba el código que llegó por correo y, si vale, se da el token
    @PostMapping("/login/verificar")
    public ResponseEntity<AuthDTOResponse> verificar(@Valid @RequestBody VerificarCodigoDTORequest request,
            HttpServletRequest peticion) {
        // Aunque el código solo tiene 6 cifras, cada desafío tiene pocos intentos y cada IP, un límite
        limitador.comprobar("verificar:" + peticion.getRemoteAddr());

        UsuarioEntity usuario = dosPasosService.verificar(request.desafio(), request.codigo());
        String rol = usuario.getRoles().iterator().next().getName();
        String token = jwtService.crearToken(usuario);

        AuthDTOResponse respuesta = new AuthDTOResponse(usuario.getId(), "Logged", usuario.getEmail(), rol, token);
        return ResponseEntity.ok().body(respuesta);
    }

}